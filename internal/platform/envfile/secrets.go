// Package envfile 在进程入口读取部署 Secret 文件，不输出文件内容。
package envfile

import (
	"fmt"
	"io"
	"os"
	"strings"
)

func Load(names ...string) error {
	for _, name := range names {
		path := os.Getenv(name + "_FILE")
		if path == "" {
			continue
		}
		if os.Getenv(name) != "" {
			return fmt.Errorf("%s and its _FILE setting are mutually exclusive", name)
		}
		f, err := os.Open(path)
		if err != nil {
			return fmt.Errorf("cannot read %s file", name)
		}
		info, err := f.Stat()
		if err != nil || !info.Mode().IsRegular() {
			f.Close()
			return fmt.Errorf("invalid %s file", name)
		}
		value, err := io.ReadAll(io.LimitReader(f, 1024*1024+1))
		f.Close()
		if err != nil || len(value) > 1024*1024 {
			return fmt.Errorf("invalid %s file size", name)
		}
		secret := strings.TrimRight(string(value), "\r\n")
		if secret == "" || strings.ContainsRune(secret, 0) {
			return fmt.Errorf("empty or invalid %s file", name)
		}
		if err = os.Setenv(name, secret); err != nil {
			return fmt.Errorf("cannot configure %s", name)
		}
	}
	return nil
}
func LoadServer() error {
	if err := Load("MND_WECHAT_OFFICIAL_TOKEN", "MND_WECHAT_OFFICIAL_ENCODING_AES_KEY"); err != nil {
		return err
	}
	return Load("MND_SEARCH_PROJECTION_TOKEN", "MND_AUDIT_PROJECTION_TOKEN", "MND_S3_ACCESS_KEY", "MND_S3_SECRET_KEY", "MND_S3_SESSION_TOKEN", "DATABASE_URL", "REDIS_URL", "JWT_SECRET", "ADMIN_KEY", "MND_OPERATORS_JSON", "SMS_GATEWAY_TOKEN", "EMAIL_GATEWAY_TOKEN", "PUSH_GATEWAY_TOKEN", "AGE_GATEWAY_TOKEN", "MND_AMAP_JS_SECURITY_CODE", "APPLE_LOGIN_CLIENT_SECRET", "APPLE_TOKEN_ENCRYPTION_KEY", "WECHAT_LOGIN_APP_SECRET", "QQ_LOGIN_APP_SECRET", "WECHAT_ANDROID_LOGIN_APP_SECRET", "WECHAT_IOS_LOGIN_APP_SECRET", "WECHAT_WEB_LOGIN_APP_SECRET", "QQ_ANDROID_LOGIN_APP_SECRET", "QQ_IOS_LOGIN_APP_SECRET", "QQ_WEB_LOGIN_APP_SECRET")
}
