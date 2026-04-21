package api

import (
	"net/http"
	"strings"

	"kitty-circle/internal/domain"
)

// GET /api/v1/search?q=keyword&type=post|listing|all
func (r *Router) handleSearch(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}
	q := strings.TrimSpace(req.URL.Query().Get("q"))
	kind := strings.ToLower(strings.TrimSpace(req.URL.Query().Get("type")))
	if kind == "" {
		kind = "all"
	}

	var posts []domain.Post
	var listings []domain.Listing
	if kind == "post" || kind == "all" {
		posts = r.store.SearchPosts(q)
		if len(posts) > 30 {
			posts = posts[:30]
		}
	}
	if kind == "listing" || kind == "all" {
		listings = r.store.SearchListings(q)
		if len(listings) > 30 {
			listings = listings[:30]
		}
	}
	writeOK(w, map[string]any{
		"query":    q,
		"type":     kind,
		"posts":    posts,
		"listings": listings,
	})
}
