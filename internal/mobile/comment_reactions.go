package mobile

import (
	"context"
	"errors"
	"fmt"
)

type CommentMetrics struct {
	CommentID   int64 `json:"comment_id"`
	Likes       int64 `json:"likes"`
	ViewerLiked bool  `json:"viewer_liked"`
}

func commentReactionKey(kind Kind, id, actor int64) string {
	return fmt.Sprintf("%s:%d:%d", kind, id, actor)
}

func (s *Service) CommentMetrics(ctx context.Context, actor int64, kind Kind, ids []int64) (out []CommentMetrics, err error) {
	out = []CommentMetrics{}
	if !HasKind([]Kind{Comments, PetComments}, kind) || len(ids) > 50 || len(Unique(ids)) != len(ids) {
		return out, Invalid("use up to 50 distinct visible comments")
	}
	err = s.Repo.View(ctx, func(tx Tx) error {
		if actor > 0 {
			if e := s.active(tx, actor); e != nil {
				return e
			}
		}
		for _, id := range ids {
			r, e := tx.Get(kind, id)
			if errors.Is(e, ErrNotFound) {
				continue
			}
			if e != nil {
				return e
			}
			visible, e := s.canRead(tx, kind, r, actor)
			if e != nil {
				return e
			}
			if !visible {
				continue
			}
			count, e := tx.Count(CommentReactions, Filter{RefID: id, State: "active"})
			if e != nil {
				return e
			}
			metric := CommentMetrics{CommentID: id, Likes: count}
			if actor > 0 {
				reaction, e := tx.ByKey(CommentReactions, commentReactionKey(kind, id, actor))
				if e == nil {
					metric.ViewerLiked = reaction.State == "active"
				} else if !errors.Is(e, ErrNotFound) {
					return e
				}
			}
			out = append(out, metric)
		}
		return nil
	})
	return
}

func (s *Service) ReactComment(ctx context.Context, actor int64, kind Kind, id int64, liked bool) (out Record, err error) {
	if !HasKind([]Kind{Comments, PetComments}, kind) {
		return out, ErrForbidden
	}
	input := struct {
		Liked bool `json:"liked"`
	}{liked}
	err = s.mutate(ctx, actor, "comment.react."+string(kind), id, input, CommentReactions, &out, func(tx Tx) error {
		if e := s.interaction(tx, actor); e != nil {
			return e
		}
		comment, e := tx.Get(kind, id)
		if e != nil {
			return e
		}
		visible, e := s.canRead(tx, kind, comment, actor)
		if e != nil {
			return e
		}
		if !visible {
			return ErrForbidden
		}
		key := commentReactionKey(kind, id, actor)
		r, e := tx.ByKey(CommentReactions, key)
		if errors.Is(e, ErrNotFound) {
			r = Record{OwnerID: actor, RefID: id, Key: key}
		} else if e != nil {
			return e
		}
		state := "removed"
		if liked {
			state = "active"
		}
		if r.ID > 0 && r.State == state {
			out = r
			return nil
		}
		r.State = state
		if e = save(tx, CommentReactions, &r, struct {
			Liked      bool `json:"liked"`
			TargetKind Kind `json:"target_kind"`
		}{liked, kind}, actor, "comment.reacted"); e != nil {
			return e
		}
		out = r
		parentKind := Entries
		if kind == PetComments {
			parentKind = Pets
		}
		if liked {
			return s.notify(tx, comment.OwnerID, actor, "interactions", parentKind, comment.RefID, "你的评论收到点赞")
		}
		return nil
	})
	return
}
