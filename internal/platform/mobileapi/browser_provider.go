package mobileapi

import (
	"bytes"
	"encoding/json"
	"io"
	"kitty-circle/internal/mobile"
	"net/http"
	"os"
	"strings"
)

func (h *BrowserHandler) webProviderID(provider string) string {
	if !mobile.HasString([]string{"wechat", "qq"}, provider) {
		return ""
	}
	prefix := strings.ToUpper(provider) + "_WEB_LOGIN_"
	if os.Getenv(prefix+"APP_SECRET") == "" || os.Getenv(prefix+"REDIRECT_URI") != h.Origin+"/app/provider-callback" {
		return ""
	}
	return os.Getenv(prefix + "APP_ID")
}

// 只接当前部署Web应用的官方凭据；固定Web设备，保留同源Cookie和CSRF校验。
func (h *BrowserHandler) webProviderBody(w http.ResponseWriter, r *http.Request, path string) ([]byte, error) {
	if r.Method != http.MethodPost || !mobile.HasString([]string{"auth/oauth-start", "auth/oauth-finish", "auth/oauth-resolve", "auth/oauth-link-scope"}, path) {
		return nil, mobile.ErrUnavailable
	}
	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, 1<<20))
	if err != nil {
		return nil, mobile.Invalid("request too large")
	}
	var fields map[string]json.RawMessage
	if json.Unmarshal(body, &fields) != nil || fields == nil {
		return nil, mobile.Invalid("invalid JSON")
	}
	if path == "auth/oauth-start" {
		var provider string
		if json.Unmarshal(fields["provider"], &provider) != nil || h.webProviderID(provider) == "" {
			return nil, mobile.ErrUnavailable
		}
	}
	if path == "auth/oauth-finish" {
		var credential map[string]json.RawMessage
		var cancelled bool
		_ = json.Unmarshal(fields["cancelled"], &cancelled)
		if json.Unmarshal(fields["credential"], &credential) != nil || credential == nil {
			return nil, mobile.Invalid("provider credential required")
		}
		if !cancelled {
			var appID string
			if json.Unmarshal(credential["app_id"], &appID) != nil || appID == "" || appID != h.webProviderID("wechat") && appID != h.webProviderID("qq") {
				return nil, mobile.ErrForbidden
			}
		}
		credential["client"] = json.RawMessage(`"web"`)
		fields["credential"], err = json.Marshal(credential)
		if err != nil {
			return nil, err
		}
	}
	return json.Marshal(fields)
}

// OAuth已有账号的会话藏在data.login中，封入HttpOnly Cookie后再返回去凭据的用户资料。
func (h *BrowserHandler) takeProviderLogin(w http.ResponseWriter, response *browserResponse) error {
	var envelope map[string]json.RawMessage
	if json.Unmarshal(response.body.Bytes(), &envelope) != nil {
		return mobile.ErrUnavailable
	}
	var result map[string]json.RawMessage
	if json.Unmarshal(envelope["data"], &result) != nil {
		return mobile.ErrUnavailable
	}
	login := result["login"]
	if len(login) == 0 || bytes.Equal(bytes.TrimSpace(login), []byte("null")) {
		return nil
	}
	flat, err := json.Marshal(map[string]json.RawMessage{"ok": json.RawMessage(`true`), "data": login})
	if err != nil {
		return err
	}
	response.body.Reset()
	_, _ = response.body.Write(flat)
	if _, err = h.takeLogin(w, response); err != nil {
		return err
	}
	var sanitized map[string]json.RawMessage
	if json.Unmarshal(response.body.Bytes(), &sanitized) != nil {
		return mobile.ErrUnavailable
	}
	result["login"] = sanitized["data"]
	envelope["data"], err = json.Marshal(result)
	if err != nil {
		return err
	}
	body, err := json.Marshal(envelope)
	if err != nil {
		return err
	}
	response.body.Reset()
	_, err = response.body.Write(body)
	return err
}
