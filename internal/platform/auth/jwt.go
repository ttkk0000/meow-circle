package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"strings"
	"time"
)

type Claims struct {
	UserID    int64  `json:"uid"`
	Username  string `json:"usr"`
	ExpiresAt int64  `json:"exp"`
	IssuedAt  int64  `json:"iat"`
}

type TokenService struct {
	secret []byte
	ttl    time.Duration
}

func NewTokenService(secret string, ttl time.Duration) *TokenService {
	return &TokenService{secret: []byte(secret), ttl: ttl}
}

func (s *TokenService) Issue(userID int64, username string) (string, error) {
	now := time.Now().UTC()
	claims := Claims{
		UserID:    userID,
		Username:  username,
		IssuedAt:  now.Unix(),
		ExpiresAt: now.Add(s.ttl).Unix(),
	}

	header := map[string]string{"alg": "HS256", "typ": "JWT"}
	headerJSON, err := json.Marshal(header)
	if err != nil {
		return "", err
	}
	payloadJSON, err := json.Marshal(claims)
	if err != nil {
		return "", err
	}

	encodedHeader := base64URL(headerJSON)
	encodedPayload := base64URL(payloadJSON)
	signingInput := encodedHeader + "." + encodedPayload

	signature := s.sign(signingInput)
	return signingInput + "." + signature, nil
}

func (s *TokenService) Parse(token string) (Claims, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return Claims{}, errors.New("invalid token format")
	}

	signingInput := parts[0] + "." + parts[1]
	expected := s.sign(signingInput)
	if !hmac.Equal([]byte(expected), []byte(parts[2])) {
		return Claims{}, errors.New("invalid token signature")
	}

	payloadBytes, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return Claims{}, errors.New("invalid token payload")
	}

	var claims Claims
	if err := json.Unmarshal(payloadBytes, &claims); err != nil {
		return Claims{}, errors.New("invalid token payload")
	}
	if time.Now().UTC().Unix() > claims.ExpiresAt {
		return Claims{}, errors.New("token expired")
	}
	return claims, nil
}

func (s *TokenService) sign(input string) string {
	mac := hmac.New(sha256.New, s.secret)
	mac.Write([]byte(input))
	return base64URL(mac.Sum(nil))
}

func base64URL(input []byte) string {
	return base64.RawURLEncoding.EncodeToString(input)
}
