package api

import (
	"net/http"
	"strconv"
	"strings"
)

func (r *Router) handleMeFollow(w http.ResponseWriter, req *http.Request) {
	path := strings.TrimPrefix(req.URL.Path, "/api/v1/me/follow/")
	path = strings.Trim(path, "/")
	targetID, err := strconv.ParseInt(path, 10, 64)
	if err != nil || targetID <= 0 {
		writeError(w, http.StatusBadRequest, "invalid user id")
		return
	}
	user, ok := currentUser(req)
	if !ok {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}
	switch req.Method {
	case http.MethodPost:
		if !r.store.Follow(user.ID, targetID) {
			writeError(w, http.StatusBadRequest, "cannot follow user")
			return
		}
		writeOK(w, map[string]any{"following": true, "user_id": targetID})
	case http.MethodDelete:
		r.store.Unfollow(user.ID, targetID)
		writeOK(w, map[string]any{"following": false, "user_id": targetID})
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}
