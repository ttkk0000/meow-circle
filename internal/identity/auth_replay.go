package identity

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"

	"kitty-circle/internal/mobile"
)

// 短期跨副本回执只保存密文，密钥与浏览器Cookie使用不同派生域。
func (s *Service) authReplayCipher() (cipher.AEAD, error) {
	if len(s.Secret) < 32 {
		return nil, mobile.ErrUnavailable
	}
	key := sha256.Sum256(append([]byte("M&D auth replay v1\x00"), s.Secret...))
	block, err := aes.NewCipher(key[:])
	if err != nil {
		return nil, err
	}
	return cipher.NewGCM(block)
}

func (s *Service) sealAuthReplay(binding string, value any) (string, error) {
	a, err := s.authReplayCipher()
	if err != nil {
		return "", err
	}
	plain, err := json.Marshal(value)
	if err != nil {
		return "", err
	}
	nonce := make([]byte, a.NonceSize())
	if _, err = rand.Read(nonce); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(a.Seal(nonce, nonce, plain, []byte(binding))), nil
}

func (s *Service) openAuthReplay(binding, value string, result any) error {
	a, err := s.authReplayCipher()
	if err != nil {
		return err
	}
	data, err := base64.RawURLEncoding.DecodeString(value)
	if err != nil || len(data) < a.NonceSize() || len(data) > 32*1024 {
		return mobile.ErrUnauthorized
	}
	plain, err := a.Open(nil, data[:a.NonceSize()], data[a.NonceSize():], []byte(binding))
	if err != nil || json.Unmarshal(plain, result) != nil {
		return mobile.ErrUnauthorized
	}
	return nil
}
