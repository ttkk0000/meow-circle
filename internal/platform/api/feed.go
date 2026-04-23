package api

import (
	"sort"
	"strings"

	"kitty-circle/internal/domain"
)

type postFeedItem struct {
	Post       domain.Post   `json:"post"`
	Author     domain.User   `json:"author"`
	LikeCount  int64         `json:"like_count"`
	Liked      bool          `json:"liked"`
	FirstMedia *domain.Media `json:"first_media,omitempty"`
}

func (r *Router) prepareFeedPosts(all []domain.Post, filter string, viewerID int64) []domain.Post {
	switch filter {
	case "follow":
		following := r.store.ListFollowingIDs(viewerID)
		set := make(map[int64]struct{}, len(following))
		for _, id := range following {
			set[id] = struct{}{}
		}
		var out []domain.Post
		for _, p := range all {
			if _, ok := set[p.AuthorID]; ok {
				out = append(out, p)
			}
		}
		sort.Slice(out, func(i, j int) bool {
			return out[i].LastReplyAt.After(out[j].LastReplyAt)
		})
		return out
	case "new":
		out := append([]domain.Post(nil), all...)
		sort.Slice(out, func(i, j int) bool {
			return out[i].CreatedAt.After(out[j].CreatedAt)
		})
		return out
	default:
		out := append([]domain.Post(nil), all...)
		ids := make([]int64, len(out))
		for i, p := range out {
			ids[i] = p.ID
		}
		counts := r.store.BatchPostLikeCounts(ids)
		sort.Slice(out, func(i, j int) bool {
			ci, cj := counts[out[i].ID], counts[out[j].ID]
			if ci != cj {
				return ci > cj
			}
			return out[i].LastReplyAt.After(out[j].LastReplyAt)
		})
		return out
	}
}

func (r *Router) buildPostFeedItems(posts []domain.Post, viewerID int64) []postFeedItem {
	if len(posts) == 0 {
		return nil
	}
	authorSeen := make(map[int64]struct{})
	var authorIDs []int64
	ids := make([]int64, len(posts))
	for i, p := range posts {
		ids[i] = p.ID
		if _, ok := authorSeen[p.AuthorID]; !ok {
			authorSeen[p.AuthorID] = struct{}{}
			authorIDs = append(authorIDs, p.AuthorID)
		}
	}
	counts := r.store.BatchPostLikeCounts(ids)
	var liked map[int64]bool
	if viewerID != 0 {
		liked = r.store.BatchUserLikedPosts(viewerID, ids)
	}
	authors := r.store.GetUsers(authorIDs)

	var mediaIDs []int64
	for _, p := range posts {
		if len(p.MediaIDs) > 0 {
			mediaIDs = append(mediaIDs, p.MediaIDs[0])
		}
	}
	mediaByID := make(map[int64]domain.Media, len(mediaIDs))
	for _, m := range r.store.GetMediaBatch(mediaIDs) {
		mediaByID[m.ID] = m
	}

	out := make([]postFeedItem, 0, len(posts))
	for _, p := range posts {
		author, ok := authors[p.AuthorID]
		if !ok {
			author = domain.User{ID: p.AuthorID, Username: "user", Nickname: ""}
		}
		var fm *domain.Media
		if len(p.MediaIDs) > 0 {
			if m, ok := mediaByID[p.MediaIDs[0]]; ok {
				mm := m
				fm = &mm
			}
		}
		lc := counts[p.ID]
		lv := false
		if liked != nil {
			lv = liked[p.ID]
		}
		out = append(out, postFeedItem{
			Post:       p,
			Author:     author,
			LikeCount:  lc,
			Liked:      lv,
			FirstMedia: fm,
		})
	}
	return out
}

func normalizeFeedFilter(s string) string {
	s = strings.ToLower(strings.TrimSpace(s))
	switch s {
	case "", "rec", "recommend":
		return "rec"
	case "new":
		return "new"
	case "follow":
		return "follow"
	default:
		return "rec"
	}
}
