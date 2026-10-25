package mobileauth

import (
	"context"
	"kitty-circle/internal/identity"
	"kitty-circle/internal/mobile"
	"net/url"
	"os"
	"strings"
)

type tencentApplication struct {
	client      Tencent
	redirectURI string
}

// AppID只是选择服务器已配置的应用；客户端不能提供Secret或改变验证地址。
type tencentApplications struct {
	apps      map[tencentApplicationKey]tencentApplication
	defaultID string
}
type tencentApplicationKey struct{ appID, platform string }

func (p tencentApplications) Verify(ctx context.Context, credential identity.Credential, nonce string) (identity.Subject, error) {
	id := credential.AppID
	if id == "" {
		id = p.defaultID // 旧客户端只可使用部署方明确配置的默认应用。
	}
	if !mobile.HasString([]string{"", "android", "ios", "web"}, credential.Client) {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	app, ok := p.apps[tencentApplicationKey{id, credential.Client}]
	if !ok && credential.Client != "web" {
		app, ok = p.apps[tencentApplicationKey{id, ""}]
	}
	if !ok {
		return identity.Subject{}, mobile.ErrUnauthorized
	}
	return app.client.verify(ctx, credential, nonce, app.redirectURI)
}

func tencentApplicationsFromEnvironment(name string) identity.Provider {
	p := tencentApplications{apps: make(map[tencentApplicationKey]tencentApplication)}
	prefix := strings.ToUpper(name)
	for _, platform := range []string{"", "ANDROID", "IOS", "WEB"} {
		key := prefix
		if platform != "" {
			key += "_" + platform
		}
		id, secret := os.Getenv(key+"_LOGIN_APP_ID"), os.Getenv(key+"_LOGIN_APP_SECRET")
		if id == "" && secret == "" {
			continue
		}
		if id == "" || secret == "" || len(id) > 128 || len(secret) > 4096 {
			return nil
		}
		redirect := ""
		if platform == "WEB" {
			redirect = os.Getenv(key + "_LOGIN_REDIRECT_URI")
			u, err := url.Parse(redirect)
			origin, originErr := url.Parse(os.Getenv("MND_WEB_ORIGIN"))
			if err != nil || originErr != nil || u.Scheme != "https" || u.Host == "" || u.User != nil || u.RawQuery != "" || u.Fragment != "" || u.Path != "/app/provider-callback" || origin.Scheme != u.Scheme || origin.Host != u.Host {
				return nil
			}
		}
		app := tencentApplication{client: Tencent{Provider: name, AppID: id, Secret: secret}, redirectURI: redirect}
		for entry, previous := range p.apps {
			if entry.appID == id && previous.client.Secret != secret {
				return nil
			}
		}
		p.apps[tencentApplicationKey{id, strings.ToLower(platform)}] = app
		if platform == "" {
			p.defaultID = id
		}
	}
	if len(p.apps) == 0 {
		return nil
	}
	return p
}
