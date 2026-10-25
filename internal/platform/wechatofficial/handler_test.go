package wechatofficial

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"encoding/base64"
	"encoding/hex"
	"encoding/xml"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strconv"
	"strings"
	"testing"
	"time"
)

// 仅回归源码，本轮未执行测试；固定值均为人工测试材料。
func fixture(t *testing.T) *Handler {
	t.Helper()
	h, err := New(Config{Enabled: true, AppID: "wx1234567890abcdef", OriginalID: "gh_fixture",
		Token: "FixtureCallbackToken123", EncodingAESKey: strings.TrimRight(base64.StdEncoding.EncodeToString(bytes.Repeat([]byte{1}, 32)), "="), WebOrigin: "https://mnd.example"})
	if err != nil {
		t.Fatal(err)
	}
	h.now = func() time.Time { return time.Unix(1792930000, 0) }
	return h
}

func callback(t *testing.T, h *Handler, plain string) (url.Values, string) {
	t.Helper()
	encrypted, err := h.encrypt([]byte(plain))
	if err != nil {
		t.Fatal(err)
	}
	stamp, nonce := strconv.FormatInt(h.now().Unix(), 10), "fixture-nonce"
	query := url.Values{"timestamp": {stamp}, "nonce": {nonce}, "encrypt_type": {"aes"},
		"msg_signature": {hex.EncodeToString(signature(h.config.Token, stamp, nonce, encrypted))}}
	return query, "<xml><ToUserName>gh_fixture</ToUserName><Encrypt>" + encrypted + "</Encrypt></xml>"
}

func serve(h *Handler, method string, query url.Values, body string) *httptest.ResponseRecorder {
	w := httptest.NewRecorder()
	h.ServeHTTP(w, httptest.NewRequest(method, CallbackPath+"?"+query.Encode(), strings.NewReader(body)))
	return w
}

func TestDisabledAndIncompleteConfiguration(t *testing.T) {
	h, err := New(Config{})
	if err != nil || serve(h, http.MethodGet, url.Values{}, "").Code != http.StatusNotFound {
		t.Fatal("disabled callback was exposed")
	}
	base := fixture(t).config
	for _, edit := range []func(*Config){
		func(c *Config) { c.AppID = "wx0000000000000000" },
		func(c *Config) { c.OriginalID = "" },
		func(c *Config) { c.Token = "" },
		func(c *Config) { c.EncodingAESKey = strings.Repeat("!", 43) },
		func(c *Config) { c.WebOrigin = "http://mnd.example" },
		func(c *Config) { c.WebOrigin = "https://user:pass@mnd.example" },
		func(c *Config) { c.WebOrigin = "https://example.invalid" },
		func(c *Config) { c.WebOrigin = "https://mnd.example/path?value=1" },
	} {
		config := base
		edit(&config)
		if _, err := New(config); err == nil {
			t.Fatal("invalid enabled configuration accepted")
		}
	}
}

func TestChallengeChecksSignatureFreshnessAndDuplicates(t *testing.T) {
	h := fixture(t)
	stamp, nonce, echo := strconv.FormatInt(h.now().Unix(), 10), "fixture", "echo12345"
	query := url.Values{"timestamp": {stamp}, "nonce": {nonce}, "echostr": {echo}, "signature": {hex.EncodeToString(signature(h.config.Token, stamp, nonce))}}
	w := serve(h, http.MethodGet, query, "")
	if w.Code != 200 || w.Body.String() != echo || w.Header().Get("Cache-Control") != "no-store" {
		t.Fatal("challenge did not return exact echo")
	}
	for _, edit := range []func(url.Values){
		func(q url.Values) { q.Set("signature", strings.Repeat("0", 40)) },
		func(q url.Values) {
			stamp := strconv.FormatInt(h.now().Add(-6*time.Minute).Unix(), 10)
			q.Set("timestamp", stamp)
			q.Set("signature", hex.EncodeToString(signature(h.config.Token, stamp, nonce)))
		},
		func(q url.Values) { q.Add("nonce", "another") },
		func(q url.Values) { q.Set("echostr", "") },
	} {
		copy, _ := url.ParseQuery(query.Encode())
		edit(copy)
		if serve(h, http.MethodGet, copy, "").Code == 200 {
			t.Fatal("invalid challenge accepted")
		}
	}
}

func TestSafeModeRepliesAreEncryptedAndHaveNoAccountSideEffects(t *testing.T) {
	h := fixture(t)
	for _, message := range []string{
		"<MsgType>text</MsgType><Content>帮助</Content>",
		"<MsgType>event</MsgType><Event>subscribe</Event>",
	} {
		query, body := callback(t, h, "<xml><ToUserName>gh_fixture</ToUserName><FromUserName>openid_fixture</FromUserName><CreateTime>1792930000</CreateTime>"+message+"</xml>")
		w := serve(h, http.MethodPost, query, body)
		var envelope struct{ Encrypt, MsgSignature, TimeStamp, Nonce string }
		if w.Code != 200 || xml.Unmarshal(w.Body.Bytes(), &envelope) != nil || !validSignature(envelope.MsgSignature, h.config.Token, envelope.TimeStamp, envelope.Nonce, envelope.Encrypt) {
			t.Fatal("invalid encrypted passive reply")
		}
		plain, err := h.decrypt(envelope.Encrypt)
		var reply struct{ ToUserName, FromUserName, MsgType, Content string }
		if err != nil || xml.Unmarshal(plain, &reply) != nil || reply.ToUserName != "openid_fixture" || reply.FromUserName != "gh_fixture" || reply.MsgType != "text" || !strings.Contains(reply.Content, "https://mnd.example/app/") {
			t.Fatal("reply recipient or public entry changed")
		}
		// 被动回复无持久化与主动发送，平台重试不会创建账号或累加业务写入。
		if repeated := serve(h, http.MethodPost, query, body); repeated.Code != 200 {
			t.Fatal("platform retry failed")
		}
	}
	query, body := callback(t, h, "<xml><ToUserName>gh_fixture</ToUserName><FromUserName>openid_fixture</FromUserName><CreateTime>1792930000</CreateTime><MsgType>event</MsgType><Event>unsubscribe</Event></xml>")
	if w := serve(h, http.MethodPost, query, body); w.Code != 200 || w.Body.String() != "success" {
		t.Fatal("unsupported event did not receive empty acknowledgement")
	}
}

func TestSafeModeRejectsDowngradeTamperingAndWrongIdentity(t *testing.T) {
	h := fixture(t)
	plain := "<xml><ToUserName>gh_fixture</ToUserName><FromUserName>openid_fixture</FromUserName><CreateTime>1792930000</CreateTime><MsgType>text</MsgType><Content>帮助</Content></xml>"
	for _, mode := range []string{"plaintext", "signature", "outer-recipient", "inner-recipient", "app-id", "duplicate-field", "oversized"} {
		t.Run(mode, func(t *testing.T) {
			query, body := callback(t, h, plain)
			switch mode {
			case "plaintext":
				query.Del("encrypt_type")
				body = plain
			case "signature":
				query.Set("msg_signature", strings.Repeat("0", 40))
			case "outer-recipient":
				body = strings.Replace(body, "gh_fixture", "gh_other", 1)
			case "inner-recipient":
				query, body = callback(t, h, strings.Replace(plain, "gh_fixture", "gh_other", 1))
			case "app-id":
				foreign := *h
				foreign.config.AppID = "wxabcdef1234567890"
				query, body = callback(t, &foreign, plain)
			case "duplicate-field":
				query, body = callback(t, h, strings.Replace(plain, "</xml>", "<MsgType>text</MsgType></xml>", 1))
			case "oversized":
				body = strings.Repeat("x", maxBody+1)
			}
			if w := serve(h, http.MethodPost, query, body); w.Code == 200 {
				t.Fatal("unsafe callback accepted")
			}
		})
	}
}

func TestCryptoRejectsBadFrameAndAllPaddingBytes(t *testing.T) {
	h := fixture(t)
	block, err := aes.NewCipher(h.key)
	if err != nil {
		t.Fatal(err)
	}
	for _, raw := range [][]byte{
		bytes.Repeat([]byte{0}, 32),                                          // 无填充。
		append(bytes.Repeat([]byte{0}, 30), 1, 2),                            // 不是全部填充字节相同。
		append(bytes.Repeat([]byte{0}, 31), 33),                              // 大于32。
		append(bytes.Repeat([]byte{0}, 16), bytes.Repeat([]byte{16}, 16)...), // 去填充后不足帧头。
	} {
		ciphertext := append([]byte(nil), raw...)
		cipher.NewCBCEncrypter(block, h.key[:aes.BlockSize]).CryptBlocks(ciphertext, ciphertext)
		if _, err := h.decrypt(base64.StdEncoding.EncodeToString(ciphertext)); err == nil {
			t.Fatal("invalid padding or frame accepted")
		}
	}
	for _, value := range []string{"!", base64.StdEncoding.EncodeToString(make([]byte, 16)), base64.StdEncoding.EncodeToString(make([]byte, maxBody+32))} {
		if _, err := h.decrypt(value); err == nil {
			t.Fatal("invalid cipher size accepted")
		}
	}
}

func TestXMLRejectsAmbiguityAndUnboundedStructures(t *testing.T) {
	for _, data := range []string{
		"<xml/><xml/>", "<!DOCTYPE xml><xml/>", "<xml xmlns='foreign'/>",
		"<xml><ToUserName>a</ToUserName><ToUserName>b</ToUserName></xml>",
		"<xml><Content><nested>help</nested></Content></xml>",
		"<xml>" + strings.Repeat("<unknown>", 17) + strings.Repeat("</unknown>", 17) + "</xml>",
		fmt.Sprintf("<xml><Content>%s</Content></xml>", strings.Repeat("x", maxBody)),
	} {
		if _, err := messageFields([]byte(data)); err == nil {
			t.Fatal("ambiguous or excessive XML accepted")
		}
	}
}
