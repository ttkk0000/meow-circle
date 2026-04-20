package api

import (
	"net/http"
	"strconv"
	"strings"
)

// PATCH /api/v1/me  {nickname, avatar_url, bio}
func (r *Router) handleUpdateMe(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodPatch && req.Method != http.MethodPut {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	user, _ := currentUser(req)
	var payload struct {
		Nickname  string `json:"nickname"`
		AvatarURL string `json:"avatar_url"`
		Bio       string `json:"bio"`
	}
	if err := decodeJSONOptional(req, &payload); err != nil {
		writeError(w, http.StatusBadRequest, err.Error())
		return
	}
	nickname := strings.TrimSpace(payload.Nickname)
	if nickname == "" {
		nickname = user.Nickname
	}
	if len(payload.Bio) > 500 {
		payload.Bio = payload.Bio[:500]
	}
	updated, ok := r.store.UpdateUserProfile(user.ID, nickname, strings.TrimSpace(payload.AvatarURL), strings.TrimSpace(payload.Bio))
	if !ok {
		writeError(w, http.StatusNotFound, "user not found")
		return
	}
	writeOK(w, updated)
}

// GET /api/v1/users/{id}  public profile
func (r *Router) handleUserPublic(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	raw := strings.TrimPrefix(req.URL.Path, "/api/v1/users/")
	raw = strings.Trim(raw, "/")
	id, err := strconv.ParseInt(raw, 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid user id")
		return
	}
	u, ok := r.store.GetUser(id)
	if !ok {
		writeError(w, http.StatusNotFound, "user not found")
		return
	}
	writeOK(w, map[string]any{
		"id":         u.ID,
		"username":   u.Username,
		"nickname":   u.Nickname,
		"avatar_url": u.AvatarURL,
		"bio":        u.Bio,
		"created_at": u.CreatedAt,
	})
}
