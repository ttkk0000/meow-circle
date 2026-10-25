package wechatofficial

import (
	"crypto/rand"
	"encoding/hex"
	"encoding/xml"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
)

func (h *Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	if !h.config.Enabled || r.URL.Path != CallbackPath {
		http.NotFound(w, r)
		return
	}
	if r.Method != http.MethodGet && r.Method != http.MethodPost {
		w.Header().Set("Allow", "GET, POST")
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}
	if len(r.URL.RawQuery) > 8192 || (r.Header.Get("Content-Encoding") != "" && r.Header.Get("Content-Encoding") != "identity") {
		http.Error(w, "invalid request", http.StatusBadRequest)
		return
	}
	query, err := url.ParseQuery(r.URL.RawQuery)
	if err != nil {
		http.Error(w, "invalid query", http.StatusBadRequest)
		return
	}
	for _, values := range query {
		if len(values) != 1 {
			http.Error(w, "ambiguous query", http.StatusBadRequest)
			return
		}
	}
	stamp, nonce := query.Get("timestamp"), query.Get("nonce")
	seconds, err := strconv.ParseInt(stamp, 10, 64)
	now := h.now()
	if err != nil || strconv.FormatInt(seconds, 10) != stamp || seconds < now.Add(-5*time.Minute).Unix() || seconds > now.Add(5*time.Minute).Unix() || !validIdentifier(nonce, 128) {
		http.Error(w, "invalid callback", http.StatusForbidden)
		return
	}
	if r.Method == http.MethodGet {
		echo := query.Get("echostr")
		if !validIdentifier(echo, 1024) || !validSignature(query.Get("signature"), h.config.Token, stamp, nonce) {
			http.Error(w, "invalid callback", http.StatusForbidden)
			return
		}
		_, _ = io.WriteString(w, echo)
		return
	}
	// 仅安全模式：签名绑定密文，禁止静默降级为正文不参与签名的明文模式。
	if query.Get("encrypt_type") != "aes" {
		http.Error(w, "safe mode required", http.StatusBadRequest)
		return
	}
	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, maxBody))
	if err != nil {
		http.Error(w, "invalid message size", http.StatusRequestEntityTooLarge)
		return
	}
	outer, err := messageFields(body)
	if err != nil || outer["ToUserName"] != h.config.OriginalID || !validSignature(query.Get("msg_signature"), h.config.Token, stamp, nonce, outer["Encrypt"]) {
		http.Error(w, "invalid callback", http.StatusForbidden)
		return
	}
	plain, err := h.decrypt(outer["Encrypt"])
	if err != nil {
		http.Error(w, "invalid callback", http.StatusForbidden)
		return
	}
	message, err := messageFields(plain)
	if err != nil || message["ToUserName"] != h.config.OriginalID || !validIdentifier(message["FromUserName"], 128) {
		http.Error(w, "invalid callback", http.StatusForbidden)
		return
	}
	created, err := strconv.ParseInt(message["CreateTime"], 10, 64)
	if err != nil || created <= 0 || !validIdentifier(message["MsgType"], 32) {
		http.Error(w, "invalid message", http.StatusBadRequest)
		return
	}
	content := h.replyContent(message)
	if content == "" {
		_, _ = io.WriteString(w, "success")
		return
	}
	reply, err := h.reply(message["FromUserName"], content, now.Unix())
	if err != nil {
		http.Error(w, "reply unavailable", http.StatusServiceUnavailable)
		return
	}
	w.Header().Set("Content-Type", "application/xml; charset=utf-8")
	_, _ = w.Write(reply)
}

func validIdentifier(value string, limit int) bool {
	if value == "" || len(value) > limit {
		return false
	}
	for _, c := range value {
		if c < 33 || c > 126 || c == '<' || c == '>' || c == '&' {
			return false
		}
	}
	return true
}

func (h *Handler) replyContent(message map[string]string) string {
	entry := "社区入口：" + h.config.WebOrigin + "/app/"
	if message["MsgType"] == "event" && message["Event"] == "subscribe" {
		return "欢迎关注 M&D · meow & doggie。一起关心猫猫，也欢迎狗狗伙伴。\n" + entry + "\n回复“帮助”查看入口。"
	}
	if message["MsgType"] == "text" {
		switch strings.ToLower(strings.TrimSpace(message["Content"])) {
		case "帮助", "菜单", "help":
			return "M&D · meow & doggie\n" + entry + "\n请在社区内查看寻宠、领养与消息；关注公众号不会自动创建或关联社区账号。"
		}
	}
	return ""
}

func (h *Handler) reply(to, content string, timestamp int64) ([]byte, error) {
	plain, err := xml.Marshal(struct {
		XMLName      xml.Name `xml:"xml"`
		ToUserName   string
		FromUserName string
		CreateTime   int64
		MsgType      string
		Content      string
	}{ToUserName: to, FromUserName: h.config.OriginalID, CreateTime: timestamp, MsgType: "text", Content: content})
	if err != nil {
		return nil, err
	}
	encrypted, err := h.encrypt(plain)
	if err != nil {
		return nil, err
	}
	var random [16]byte
	if _, err = rand.Read(random[:]); err != nil {
		return nil, err
	}
	stamp, nonce := strconv.FormatInt(timestamp, 10), hex.EncodeToString(random[:])
	return xml.Marshal(struct {
		XMLName      xml.Name `xml:"xml"`
		Encrypt      string
		MsgSignature string
		TimeStamp    string
		Nonce        string
	}{Encrypt: encrypted, MsgSignature: hex.EncodeToString(signature(h.config.Token, stamp, nonce, encrypted)), TimeStamp: stamp, Nonce: nonce})
}
