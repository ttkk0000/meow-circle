package mobile_test

import (
	"context"
	"errors"
	"kitty-circle/internal/mobile"
	"testing"
	"time"
)

// 仅回归源码：既有成人消息请求不能绕过后续年龄复核；本轮未执行。
func TestProtectedDirectContactRechecksProofAndKeepsFriendshipReview(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	old, err := s.Conversation(ctx, 1, mobile.ConversationInput{CommandMeta: meta(), UserID: 2})
	if err != nil {
		t.Fatal(err)
	}
	accepted, err := s.ConversationAction(ctx, 2, old.ID, old.Version, "accept")
	if err != nil {
		t.Fatal(err)
	}
	if err = s.RecordAgeEvidence(ctx, "fixture-verifier", mobile.AgeEvidenceInput{ActorID: 2, AgeBand: "14_17", ValidUntil: s.Now().Add(time.Hour), EvidenceRef: "updated-reviewed-age"}); err != nil {
		t.Fatal(err)
	}
	if _, err = s.Conversation(ctx, 3, mobile.ConversationInput{CommandMeta: meta(), UserID: 2}); err == nil {
		t.Fatal("stranger opened a protected direct request")
	}
	if _, err = s.Message(ctx, 1, accepted.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "new message"}, IdempotencyKey: "protected-direct-fixture-001"}); err == nil {
		t.Fatal("old accepted conversation bypassed updated age proof")
	}
	var rule mobile.RuleError
	if !errors.As(err, &rule) || rule.Code != "contact_restricted" {
		t.Fatal("protected contact exposed recipient classification", err)
	}
	request, err := s.Friend(ctx, 1, "request", mobile.FriendInput{CommandMeta: meta(), UserID: 2})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Friend(ctx, 2, "accept", mobile.FriendInput{CommandMeta: meta(), UserID: 1, Version: request.Version}); err != nil {
		t.Fatal(err)
	}
	if _, err = s.Message(ctx, 1, accepted.ID, false, mobile.MessageInput{Content: mobile.RichContent{Text: "protected friend conversation"}, IdempotencyKey: "protected-direct-fixture-002"}); err != nil {
		t.Fatal("confirmed friendship could not continue protected chat", err)
	}
	if err = repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.Qualifications, "2")
		if e != nil {
			return e
		}
		q, e := mobile.Decode[mobile.Qualification](r)
		if e != nil {
			return e
		}
		q.AgeValidUntil = s.Now().Add(-time.Second)
		if e = mobile.SetBody(&r, q); e != nil {
			return e
		}
		return tx.Save(mobile.Qualifications, &r)
	}); err != nil {
		t.Fatal(err)
	}
	if _, err = s.Friend(ctx, 2, "block", mobile.FriendInput{CommandMeta: meta(), UserID: 3}); err != nil {
		t.Fatal("expired interaction eligibility blocked protective action", err)
	}
}

func TestExpiredProofCannotAcceptStrangerRequestButCanDecline(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	request, err := s.Conversation(ctx, 1, mobile.ConversationInput{CommandMeta: meta(), UserID: 2})
	if err != nil {
		t.Fatal(err)
	}
	if err = repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.Qualifications, "1")
		if e != nil {
			return e
		}
		q, e := mobile.Decode[mobile.Qualification](r)
		if e != nil {
			return e
		}
		q.AgeValidUntil = s.Now().Add(-time.Second)
		if e = mobile.SetBody(&r, q); e != nil {
			return e
		}
		return tx.Save(mobile.Qualifications, &r)
	}); err != nil {
		t.Fatal(err)
	}
	if _, err = s.ConversationAction(ctx, 2, request.ID, request.Version, "accept"); err == nil {
		t.Fatal("expired stranger proof accepted")
	}
	if _, err = s.ConversationAction(ctx, 2, request.ID, request.Version, "decline"); err != nil {
		t.Fatal("protective decline disabled", err)
	}
}
