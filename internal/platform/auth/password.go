package auth

import (
	"crypto/pbkdf2"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
)

const (
	saltSize   = 16
	iterations = 100_000
	keySize    = 32
)

func HashPassword(password string) (hash string, salt string, err error) {
	saltBytes := make([]byte, saltSize)
	if _, err := rand.Read(saltBytes); err != nil {
		return "", "", err
	}
	saltHex := hex.EncodeToString(saltBytes)
	hashBytes, err := pbkdf2.Key(sha256.New, password, saltBytes, iterations, keySize)
	if err != nil {
		return "", "", err
	}
	return hex.EncodeToString(hashBytes), saltHex, nil
}

func VerifyPassword(password, hash, salt string) bool {
	saltBytes, err := hex.DecodeString(salt)
	if err != nil {
		return false
	}
	expected, err := hex.DecodeString(hash)
	if err != nil {
		return false
	}
	got, err := pbkdf2.Key(sha256.New, password, saltBytes, iterations, keySize)
	if err != nil {
		return false
	}
	return subtle.ConstantTimeCompare(expected, got) == 1
}
