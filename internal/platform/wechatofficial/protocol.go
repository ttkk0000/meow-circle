package wechatofficial

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha1" // 微信回调协议规定的摘要算法，不用于密码或应用会话。
	"crypto/subtle"
	"encoding/base64"
	"encoding/binary"
	"encoding/hex"
	"encoding/xml"
	"errors"
	"io"
	"sort"
	"strings"
)

const maxBody = 64 << 10

var errMessage = errors.New("invalid WeChat official account message")

func signature(parts ...string) []byte {
	sort.Strings(parts)
	digest := sha1.Sum([]byte(strings.Join(parts, "")))
	return digest[:]
}

func validSignature(value string, parts ...string) bool {
	digest, err := hex.DecodeString(value)
	return err == nil && len(digest) == sha1.Size && subtle.ConstantTimeCompare(digest, signature(parts...)) == 1
}

func (h *Handler) decrypt(value string) ([]byte, error) {
	data, err := base64.StdEncoding.Strict().DecodeString(value)
	if err != nil || len(data) < 32 || len(data) > maxBody || len(data)%32 != 0 {
		return nil, errMessage
	}
	block, err := aes.NewCipher(h.key)
	if err != nil {
		return nil, errMessage
	}
	cipher.NewCBCDecrypter(block, h.key[:aes.BlockSize]).CryptBlocks(data, data)
	padding := int(data[len(data)-1])
	if padding < 1 || padding > 32 || padding > len(data) {
		return nil, errMessage
	}
	for _, value := range data[len(data)-padding:] {
		if int(value) != padding {
			return nil, errMessage
		}
	}
	data = data[:len(data)-padding]
	if len(data) < 20 {
		return nil, errMessage
	}
	size := uint64(binary.BigEndian.Uint32(data[16:20]))
	if size > uint64(len(data)-20) {
		return nil, errMessage
	}
	end := 20 + int(size)
	if subtle.ConstantTimeCompare(data[end:], []byte(h.config.AppID)) != 1 {
		return nil, errMessage
	}
	return data[20:end], nil
}

func (h *Handler) encrypt(message []byte) (string, error) {
	data := make([]byte, 20, 20+len(message)+len(h.config.AppID)+32)
	if _, err := rand.Read(data[:16]); err != nil {
		return "", err
	}
	binary.BigEndian.PutUint32(data[16:20], uint32(len(message)))
	data = append(data, message...)
	data = append(data, h.config.AppID...)
	padding := 32 - len(data)%32
	data = append(data, bytes.Repeat([]byte{byte(padding)}, padding)...)
	block, err := aes.NewCipher(h.key)
	if err != nil {
		return "", err
	}
	cipher.NewCBCEncrypter(block, h.key[:aes.BlockSize]).CryptBlocks(data, data)
	return base64.StdEncoding.EncodeToString(data), nil
}

// 读取完整XML并限制嵌套，拒绝重复字段、命名空间、DTD与尾随第二个根节点。
// 未使用的图片／位置等字段仅做结构校验，不存储或写日志。
func messageFields(data []byte) (map[string]string, error) {
	if len(data) == 0 || len(data) > maxBody {
		return nil, errMessage
	}
	d := xml.NewDecoder(bytes.NewReader(data))
	fields := make(map[string]string)
	seen := make(map[string]bool)
	depth, roots := 0, 0
	field := ""
	known := map[string]bool{"ToUserName": true, "FromUserName": true, "CreateTime": true, "MsgType": true, "Content": true, "Event": true, "Encrypt": true}
	for {
		token, err := d.Token()
		if err == io.EOF {
			if roots != 1 || depth != 0 {
				return nil, errMessage
			}
			return fields, nil
		}
		if err != nil {
			return nil, errMessage
		}
		switch value := token.(type) {
		case xml.StartElement:
			depth++
			if depth > 16 || value.Name.Space != "" || len(value.Attr) != 0 || (depth > 2 && field != "") {
				return nil, errMessage
			}
			if depth == 1 {
				roots++
				if roots != 1 || value.Name.Local != "xml" {
					return nil, errMessage
				}
			}
			if depth == 2 {
				if seen[value.Name.Local] {
					return nil, errMessage
				}
				seen[value.Name.Local] = true
				if known[value.Name.Local] {
					field = value.Name.Local
				}
			}
		case xml.EndElement:
			if depth == 2 {
				field = ""
			}
			depth--
		case xml.CharData:
			if depth == 2 && field != "" {
				fields[field] += string(value)
			} else if depth <= 1 && len(bytes.TrimSpace(value)) != 0 {
				return nil, errMessage
			}
		case xml.Directive:
			return nil, errMessage
		case xml.ProcInst:
			if roots != 0 || value.Target != "xml" {
				return nil, errMessage
			}
		}
	}
}
