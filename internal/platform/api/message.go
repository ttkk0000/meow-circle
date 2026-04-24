package api

import (
	"net/http"
	"strconv"
	"strings"

	"kitty-circle/internal/domain"
)

// GET  /api/v1/me/conversations               list my conversations
// GET  /api/v1/me/conversations/{peerID}      fetch + mark-as-read conversation with peer
// POST /api/v1/messages                        send a new message

func (r *Router) handleMyConversations(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	user, _ := currentUser(req)
	writeOK(w, map[string]any{"items": r.store.ListConversations(user.ID)})
}

func (r *Router) handleConversationWithPeer(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	user, _ := currentUser(req)
	raw := strings.TrimPrefix(req.URL.Path, "/api/v1/me/conversations/")
	raw = strings.Trim(raw, "/")
	peerID, err := strconv.ParseInt(raw, 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid peer id")
		return
	}
	peer, ok := r.store.GetUser(peerID)
	if !ok {
		writeError(w, http.StatusNotFound, "peer not found")
		return
	}
	msgs := r.store.ListMessagesBetween(user.ID, peerID)
	r.store.MarkConversationRead(user.ID, peerID)
	writeOK(w, map[string]any{
		"peer": map[string]any{
			"id": peer.ID, "username": peer.Username, "nickname": peer.Nickname,
			"avatar_url": peer.AvatarURL,
		},
		"messages": msgs,
	})
}

func (r *Router) handleSendMessage(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	user, _ := currentUser(req)
	var payload struct {
		RecipientID int64  `json:"recipient_id"`
		Content     string `json:"content"`
	}
	if err := decodeJSON(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	content := strings.TrimSpace(payload.Content)
	if payload.RecipientID <= 0 || content == "" {
		writeError(w, http.StatusBadRequest, "recipient_id and content are required")
		return
	}
	if payload.RecipientID == user.ID {
		writeError(w, http.StatusBadRequest, "cannot message yourself")
		return
	}
	if _, ok := r.store.GetUser(payload.RecipientID); !ok {
		writeError(w, http.StatusNotFound, "recipient not found")
		return
	}
	if hit, blocked := r.filter.Check(content); blocked {
		writeError(w, http.StatusBadRequest, "content rejected by moderation: "+hit)
		return
	}
	if len(content) > 2000 {
		content = content[:2000]
	}

	msg := r.store.CreateMessage(domain.Message{
		SenderID:    user.ID,
		RecipientID: payload.RecipientID,
		Content:     content,
	})
	var opts []NotifyOption
	if sender, ok := r.store.GetUser(user.ID); ok {
		opts = append(opts, notifyActor(sender))
	}
	r.notify(payload.RecipientID, domain.NotificationMessage, "New message", content, user.ID, opts...)
	writeCreated(w, msg)
}
