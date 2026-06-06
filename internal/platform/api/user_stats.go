package api

import (
	"net/http"
	"strconv"
	"strings"
)

// GET /api/v1/users/{id}/stats
func (r *Router) handleUserStats(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	// URL layout is /api/v1/users/{id}/stats
	// Trim prefix "/api/v1/users/" and suffix "/stats"
	path := strings.TrimPrefix(req.URL.Path, "/api/v1/users/")
	path = strings.TrimSuffix(path, "/stats")
	path = strings.Trim(path, "/")
	id, err := strconv.ParseInt(path, 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid user id")
		return
	}
	_, ok := r.store.GetUser(id)
	if !ok {
		writeError(w, http.StatusNotFound, "user not found")
		return
	}
	followers := r.store.CountFollowers(id)
	following := r.store.CountFollowing(id)
	writeOK(w, map[string]any{
		"followers": followers,
		"following": following,
	})
}
