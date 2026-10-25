// Package wechatofficial 处理公众号被动回调，不参与消费者登录或账号关联。
package wechatofficial

import (
	"encoding/base64"
	"fmt"
	"net/http"
	"net/url"
	"os"
	"regexp"
	"strings"
	"time"
)

const CallbackPath = "/integrations/wechat/official/callback"

type Config struct {
	Enabled        bool
	AppID          string
	OriginalID     string
	Token          string
	EncodingAESKey string
	WebOrigin      string
}

type Handler struct {
	config Config
	key    []byte
	now    func() time.Time
}

// New 默认关闭；启用但配置不完整时拒绝启动，避免平台校验成功而消息被静默丢弃。
func New(config Config) (*Handler, error) {
	h := &Handler{config: config, now: time.Now}
	if !config.Enabled {
		return h, nil
	}
	key, err := base64.StdEncoding.Strict().DecodeString(config.EncodingAESKey + "=")
	origin, originErr := url.Parse(config.WebOrigin)
	if !regexp.MustCompile(`^wx[A-Za-z0-9]{16}$`).MatchString(config.AppID) || config.AppID == "wx0000000000000000" ||
		!regexp.MustCompile(`^gh_[A-Za-z0-9_]{1,64}$`).MatchString(config.OriginalID) ||
		!regexp.MustCompile(`^[A-Za-z0-9]{3,32}$`).MatchString(config.Token) ||
		len(config.EncodingAESKey) != 43 || err != nil || len(key) != 32 ||
		originErr != nil || origin.Scheme != "https" || origin.Hostname() == "" || origin.User != nil ||
		origin.Opaque != "" || origin.RawQuery != "" || origin.ForceQuery || origin.Fragment != "" ||
		(origin.Path != "" && origin.Path != "/") || strings.HasSuffix(origin.Hostname(), ".invalid") {
		return nil, fmt.Errorf("invalid WeChat official account callback configuration")
	}
	h.key = key
	h.config.WebOrigin = strings.TrimRight(config.WebOrigin, "/")
	return h, nil
}

func FromEnvironment() (http.Handler, error) {
	enabled := os.Getenv("MND_WECHAT_OFFICIAL_ENABLED")
	if enabled != "" && enabled != "0" && enabled != "1" {
		return nil, fmt.Errorf("MND_WECHAT_OFFICIAL_ENABLED must be 0 or 1")
	}
	return New(Config{
		Enabled: enabled == "1", AppID: os.Getenv("MND_WECHAT_OFFICIAL_APP_ID"),
		OriginalID: os.Getenv("MND_WECHAT_OFFICIAL_ORIGINAL_ID"), Token: os.Getenv("MND_WECHAT_OFFICIAL_TOKEN"),
		EncodingAESKey: os.Getenv("MND_WECHAT_OFFICIAL_ENCODING_AES_KEY"), WebOrigin: os.Getenv("MND_WEB_ORIGIN"),
	})
}
