package mobile_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// 本批仅提供关键回归源码，未执行测试。
func TestContentFollowingDoesNotGrantPrivateAudienceAndSettingsPreserveIt(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	_, err := s.SaveEntry(ctx, 1, 0, declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Space: "community", Text: "公开日常", City: "武汉", Audience: mobile.Audience{Mode: "public"}}}))
	if err != nil {
		t.Fatal(err)
	}
	private, err := s.SaveEntry(ctx, 1, 0, declared(t, s, mobile.EntryInput{Entry: mobile.Entry{Space: "diary", Text: "私密日记", Audience: mobile.Audience{Mode: "private"}}}))
	if err != nil {
		t.Fatal(err)
	}
	in := mobile.ContentSubscriptionInput{CommandMeta: meta(), FollowedUsers: []int64{1}, WatchedCases: []int64{}}
	r, err := s.SaveContentSubscriptions(ctx, 2, in)
	if err != nil {
		t.Fatal(err)
	}
	replayed, err := s.SaveContentSubscriptions(ctx, 2, in)
	if err != nil || replayed.ID != r.ID || replayed.Version != r.Version {
		t.Fatalf("idempotency: %v", err)
	}
	if _, err = s.Get(ctx, 2, mobile.Entries, private.ID); !errors.Is(err, mobile.ErrNotFound) {
		t.Fatalf("private access: %v", err)
	}
	page, err := s.ListFiltered(ctx, 2, mobile.Entries, 0, 0, 30, mobile.Query{Following: true, City: "武汉"})
	if err != nil || len(page.Items) != 1 {
		t.Fatalf("following page: %v %#v", err, page)
	}
	_, err = s.Settings(ctx, 2, mobile.Preferences, mobile.SettingsInput{Version: r.Version, Preference: &mobile.Preference{Frequency: "off", Content: &mobile.ContentSubscriptions{FollowedUsers: []int64{3}}}})
	if err != nil {
		t.Fatal(err)
	}
	r, err = s.ContentSubscriptions(ctx, 2)
	if err != nil {
		t.Fatal(err)
	}
	v := body[mobile.ContentSubscriptions](t, r)
	if len(v.FollowedUsers) != 1 || v.FollowedUsers[0] != 1 {
		t.Fatal("notification settings changed follows", v)
	}
}

func TestDirectoryProjectionAndPendingRequestsFollowCurrentGroupOwner(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	g, err := s.CreateGroup(ctx, 1, mobile.GroupInput{CommandMeta: meta(), Name: "同城宠友", Discoverable: true, City: "武汉", Summary: "公开介绍，不含成员资料"})
	if err != nil {
		t.Fatal(err)
	}
	preview, err := s.GroupDirectory(ctx, 0, 0, g.ID, "", "")
	if err != nil || len(preview.Items) != 1 || len(preview.Items[0].Participants) != 0 {
		t.Fatalf("projection: %v", err)
	}
	if _, err = s.Get(ctx, 3, mobile.Groups, g.ID); !errors.Is(err, mobile.ErrNotFound) {
		t.Fatalf("directory granted membership: %v", err)
	}
	join := func(actor int64) mobile.Record {
		t.Helper()
		r, e := s.RequestGroupJoin(ctx, actor, g.ID, mobile.GroupJoinInput{CommandMeta: meta(), Note: "希望交流日常"})
		if e != nil {
			t.Fatal(e)
		}
		return r
	}
	first := join(2)
	if _, err = s.Invitation(ctx, 1, first.ID, first.Version, "accept"); err != nil {
		t.Fatal(err)
	}
	pending := join(3)
	g, err = s.Get(ctx, 1, mobile.Groups, g.ID)
	if err != nil {
		t.Fatal(err)
	}
	g, err = s.Group(ctx, 1, g.ID, "transfer", mobile.GroupAction{CommandMeta: meta(), Version: g.Version, UserID: 2})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Group(ctx, 2, g.ID, "accept_transfer", mobile.GroupAction{CommandMeta: meta(), Version: g.Version}); err != nil {
		t.Fatal(err)
	}
	if _, err = s.Get(ctx, 1, mobile.Invitations, pending.ID); !errors.Is(err, mobile.ErrNotFound) {
		t.Fatalf("old owner retained pending materials: %v", err)
	}
	pending, err = s.Get(ctx, 2, mobile.Invitations, pending.ID)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = s.Invitation(ctx, 2, pending.ID, pending.Version, "accept"); err != nil {
		t.Fatal(err)
	}
	if _, err = s.Get(ctx, 3, mobile.Groups, g.ID); err != nil {
		t.Fatal(err)
	}
}

func TestOtherPetsHaveProfilesButCannotEnterCatDogAdoption(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	in := mobile.PetInput{CommandMeta: meta(), Pet: mobile.Pet{Name: "兔兔", Species: "rabbit", Audience: mobile.Audience{Mode: "public"}}}
	r, err := s.SavePet(ctx, 1, 0, in)
	if err != nil {
		t.Fatal(err)
	}
	in.CommandMeta, in.Version, in.Pet.AdoptionAvailable = meta(), r.Version, true
	if _, err = s.SavePet(ctx, 1, r.ID, in); err == nil {
		t.Fatal("rabbit entered cat/dog adoption")
	}
}

func TestWatchedCaseNoticesArePerVersionAndStopAfterUnsubscribe(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	c, err := s.SaveCase(ctx, 1, 0, mobile.CaseInput{CommandMeta: meta(), Case: mobile.Case{Type: "lost", Title: "猫咪走失", Species: "cat", Description: "公开大致区域", City: "武汉", Latitude: 30.5, Longitude: 114.3, OccurredAt: time.Now().Add(-time.Hour)}})
	if err != nil {
		t.Fatal(err)
	}
	p, err := s.SaveContentSubscriptions(ctx, 2, mobile.ContentSubscriptionInput{CommandMeta: meta(), WatchedCases: []int64{c.ID}})
	if err != nil {
		t.Fatal(err)
	}
	first, err := s.CaseWatchAlerts(ctx, c.ID, 0)
	if err != nil || first.Created != 1 {
		t.Fatalf("first: %v %#v", err, first)
	}
	again, err := s.CaseWatchAlerts(ctx, c.ID, 0)
	if err != nil || again.Created != 0 {
		t.Fatalf("replayed: %v %#v", err, again)
	}
	if _, err = s.CloseCase(ctx, 1, c.ID, mobile.StateInput{CommandMeta: meta(), Version: c.Version, Note: "已找到"}); err != nil {
		t.Fatal(err)
	}
	closed, err := s.CaseWatchAlerts(ctx, c.ID, 0)
	if err != nil || closed.Created != 1 {
		t.Fatalf("closed: %v %#v", err, closed)
	}
	if _, err = s.SaveContentSubscriptions(ctx, 2, mobile.ContentSubscriptionInput{CommandMeta: meta(), Version: p.Version}); err != nil {
		t.Fatal(err)
	}
	last, err := s.CaseWatchAlerts(ctx, c.ID, 0)
	if err != nil || last.Created != 0 {
		t.Fatalf("unsubscribed: %v %#v", err, last)
	}
}
