package mobile_test

import (
	"context"
	"errors"
	"fmt"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// Regression source only. These tests have not been executed.
func serverRecord(t *testing.T, repo mobile.Repository, kind mobile.Kind, r mobile.Record, value any) mobile.Record {
	t.Helper()
	if e := repo.Update(context.Background(), func(tx mobile.Tx) error {
		if e := mobile.SetBody(&r, value); e != nil {
			return e
		}
		return tx.Save(kind, &r)
	}); e != nil {
		t.Fatal(e)
	}
	return r
}

func TestLegacyMutationRejectsSessionRevokedAfterAuthentication(t *testing.T) {
	s, repo := fixture(t)
	ctx, _ := sessionAndGrant(t, repo, 1, mobile.PurposeScope{})
	if e := repo.Update(ctx, func(tx mobile.Tx) error {
		r, e := tx.ByKey(mobile.Sessions, mobile.SessionKey(ctx))
		if e != nil {
			return e
		}
		r.State = "revoked"
		return tx.Save(mobile.Sessions, &r)
	}); e != nil {
		t.Fatal(e)
	}
	_, e := s.Settings(ctx, 1, mobile.Preferences, mobile.SettingsInput{Preference: &mobile.Preference{Frequency: "off"}})
	if !errors.Is(e, mobile.ErrUnauthorized) {
		t.Fatal("stale HTTP session wrote settings", e)
	}
}

func TestOptionalMutationReceiptSurvivesDeletedResult(t *testing.T) {
	s, repo := fixture(t)
	ctx := mobile.WithCommandID(context.Background(), "settings-receipt-fixture")
	in := mobile.SettingsInput{Preference: &mobile.Preference{Frequency: "off"}}
	first, e := s.Settings(ctx, 1, mobile.Preferences, in)
	if e != nil {
		t.Fatal(e)
	}
	again, e := s.Settings(ctx, 1, mobile.Preferences, in)
	if e != nil || again.ID != first.ID || again.Version != first.Version {
		t.Fatal("lost-response replay mutated twice", again, e)
	}
	in.Preference.Frequency = "instant"
	if _, e = s.Settings(ctx, 1, mobile.Preferences, in); e == nil {
		t.Fatal("same key accepted a different payload")
	}
	if e = repo.Update(ctx, func(tx mobile.Tx) error { return tx.Delete(mobile.Preferences, first.ID, first.Version) }); e != nil {
		t.Fatal(e)
	}
	receipt, e := s.Operation(ctx, 1, "settings-receipt-fixture")
	if e != nil || receipt.State != "committed" || receipt.ResultID != first.ID || receipt.PayloadHash != "" {
		t.Fatal(receipt, e)
	}
	if _, e = s.Operation(ctx, 2, "settings-receipt-fixture"); !errors.Is(e, mobile.ErrNotFound) {
		t.Fatal("other actor read receipt", e)
	}
	if _, e = mobile.CommandContext(ctx, "different-request-key"); e == nil {
		t.Fatal("header/body mismatch accepted")
	}
}

func TestRepeatedLegacyLeadHasOneRecordAndOneNotice(t *testing.T) {
	s, repo := fixture(t)
	c := serverRecord(t, repo, mobile.Cases, mobile.Record{OwnerID: 1, Public: true, State: "active"}, mobile.Case{Species: "cat"})
	a, e := s.Lead(context.Background(), 2, c.ID, "同一条线索", nil)
	if e != nil {
		t.Fatal(e)
	}
	b, e := s.Lead(context.Background(), 2, c.ID, "同一条线索", nil)
	if e != nil || a.ID != b.ID {
		t.Fatal("duplicate lead", b, e)
	}
	if e = repo.View(context.Background(), func(tx mobile.Tx) error {
		n, e := tx.Count(mobile.Notifications, mobile.Filter{OwnerID: 1})
		if e != nil {
			return e
		}
		if n != 1 {
			t.Fatalf("duplicate notice: %d", n)
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
}

func TestFeedbackRequiresExistingEffectiveTermsAndDeduplicates(t *testing.T) {
	s, repo := fixture(t)
	a := mobile.Appointment{EffectiveVersion: 1, Versions: []mobile.AppointmentVersion{{Number: 1, Status: "effective", Terms: mobile.AppointmentTerms{StartsAt: time.Now().Add(-time.Hour)}}}}
	r := serverRecord(t, repo, mobile.Appointments, mobile.Record{OwnerID: 1, Participants: []int64{2}, State: "confirmed"}, a)
	in := mobile.Feedback{AppointmentID: r.ID, Version: 1, Items: map[string]string{"identity": "match", "pet": "match", "reception": "match", "place": "match", "fees": "match"}}
	first, e := s.Feedback(context.Background(), 2, in)
	if e != nil {
		t.Fatal(e)
	}
	again, e := s.Feedback(context.Background(), 2, in)
	if e != nil || first.ID != again.ID {
		t.Fatal("duplicate feedback", again, e)
	}
	in.Note = "different"
	if _, e = s.Feedback(context.Background(), 2, in); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("conflicting feedback replaced original", e)
	}
	a.Versions = nil
	r = serverRecord(t, repo, mobile.Appointments, r, a)
	if _, e = s.Feedback(context.Background(), 1, in); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("missing effective terms accepted", e)
	}
}

func TestDataProcessingBoundsMutationsAndExcludesProgressEvents(t *testing.T) {
	s, repo := fixture(t)
	d := mobile.DataRequest{Type: "erase_fields", ExternalState: "not_required"}
	for i := 0; i < 101; i++ {
		r := serverRecord(t, repo, mobile.Drafts, mobile.Record{OwnerID: 1, Key: fmt.Sprint(i), State: "active"}, mobile.Draft{Text: "private"})
		d.Items = append(d.Items, mobile.DataItem{Kind: mobile.Drafts, ID: r.ID, State: "pending"})
	}
	r := serverRecord(t, repo, mobile.DataRequests, mobile.Record{OwnerID: 1, State: "processing"}, d)
	first, e := s.ProcessDataRequest(context.Background(), "privacy-fixture", r.ID, r.Version)
	if e != nil {
		t.Fatal(e)
	}
	if first.State != "processing" {
		t.Fatal("pending batch was marked completed or legally held", first.State)
	}
	if e = repo.View(context.Background(), func(tx mobile.Tx) error {
		n, e := tx.Count(mobile.Drafts, mobile.Filter{OwnerID: 1})
		if e != nil {
			return e
		}
		if n != 1 {
			t.Fatalf("expected one remaining target, got %d", n)
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
	last, e := s.ProcessDataRequest(context.Background(), "privacy-fixture", r.ID, first.Version)
	if e != nil || last.State != "completed" {
		t.Fatal(last, e)
	}
	work, e := s.ClaimDeliveries(context.Background(), "privacy-fixture", mobile.DeliveryClaim{Consumer: "privacy_cleanup", Limit: 50})
	if e != nil || len(work) != 0 {
		t.Fatal("progress event recursively generated cleanup jobs", len(work), e)
	}
}

type fixturePush struct {
	sends        int
	accepted     map[string]mobile.PushAcceptance
	beforeLookup func()
}

func (p *fixturePush) Lookup(_ context.Context, key string) (mobile.PushLookup, error) {
	if p.beforeLookup != nil {
		p.beforeLookup()
		p.beforeLookup = nil
	}
	if receipt, ok := p.accepted[key]; ok {
		return mobile.PushLookup{State: "accepted", Receipt: receipt}, nil
	}
	return mobile.PushLookup{State: "not_found"}, nil
}
func (p *fixturePush) Send(_ context.Context, in mobile.PushPayload) (mobile.PushAcceptance, error) {
	p.sends++
	receipt := mobile.PushAcceptance{MessageID: "fixture-accepted", Accepted: true}
	if p.accepted == nil {
		p.accepted = map[string]mobile.PushAcceptance{}
	}
	p.accepted[in.IdempotencyKey] = receipt
	return receipt, nil
}
func pushNotice(t *testing.T, s *mobile.Service, repo mobile.Repository) mobile.Record {
	t.Helper()
	serverRecord(t, repo, mobile.Preferences, mobile.Record{OwnerID: 1, Key: "1", State: "active"}, mobile.Preference{PushPermission: true, Frequency: "instant", Interactions: true})
	if _, e := s.RegisterPushDevice(context.Background(), 1, mobile.PushDeviceInput{Platform: "android", Token: "synthetic-device-token-123", Permission: "granted", Enabled: true}); e != nil {
		t.Fatal(e)
	}
	return serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 1, State: "unread"}, mobile.Notification{Category: "interactions", PushState: "queued"})
}

func TestPushAcceptanceReplayDoesNotSendTwice(t *testing.T) {
	s, repo := fixture(t)
	r := pushNotice(t, s, repo)
	p := &fixturePush{}
	s.Push = p
	if e := s.DispatchPush(context.Background(), "worker-fixture", r.ID); e != nil {
		t.Fatal(e)
	}
	// Completed work may be retried after a lease/response was lost.
	s.Push = nil
	if e := s.DispatchPush(context.Background(), "worker-fixture", r.ID); e != nil {
		t.Fatal(e)
	}
	if p.sends != 1 {
		t.Fatal(p.sends)
	}
}

func TestPushRechecksPermissionBetweenLookupAndSend(t *testing.T) {
	s, repo := fixture(t)
	r := pushNotice(t, s, repo)
	p := &fixturePush{beforeLookup: func() {
		if e := repo.Update(context.Background(), func(tx mobile.Tx) error { return mobile.DisablePushDevices(tx, 1, "", true) }); e != nil {
			t.Fatal(e)
		}
	}}
	s.Push = p
	if e := s.DispatchPush(context.Background(), "worker-fixture", r.ID); e == nil || p.sends != 0 {
		t.Fatal("revoked device received push", p.sends, e)
	}
}

func TestDisabledPushTokenReusePreservesPreviousOwnerInventory(t *testing.T) {
	s, repo := fixture(t)
	in := mobile.PushDeviceInput{Platform: "ios", Token: "synthetic-reused-token-123", Permission: "granted", Enabled: true}
	old, e := s.RegisterPushDevice(context.Background(), 1, in)
	if e != nil {
		t.Fatal(e)
	}
	if e = repo.Update(context.Background(), func(tx mobile.Tx) error { return mobile.DisablePushDevices(tx, 1, "", true) }); e != nil {
		t.Fatal(e)
	}
	fresh, e := s.RegisterPushDevice(context.Background(), 2, in)
	if e != nil || old.ID == fresh.ID {
		t.Fatal(fresh, e)
	}
	if e = repo.View(context.Background(), func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.PushDevices, old.ID)
		if e != nil {
			return e
		}
		if r.OwnerID != 1 || r.State != "disabled" {
			t.Fatal("old cleanup inventory changed owner", r)
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
}

func TestNearbyDeliveryContinuationIsNotAcknowledgedEarly(t *testing.T) {
	s, repo := fixture(t)
	now := time.Now().UTC()
	s.Now = func() time.Time { return now }
	serverRecord(t, repo, mobile.Outbox, mobile.Record{State: "pending"}, mobile.Event{SchemaVersion: 1, AggregateKind: mobile.Cases, AggregateID: 42, Topic: "case.saved"})
	work, e := s.ClaimDeliveries(context.Background(), "worker-fixture", mobile.DeliveryClaim{Consumer: "nearby_alerts", Limit: 1})
	if e != nil || len(work) != 1 {
		t.Fatal(work, e)
	}
	if e = s.CompleteDelivery(context.Background(), "worker-fixture", work[0].Delivery.ID, mobile.DeliveryResult{Consumer: "nearby_alerts", Version: work[0].Delivery.Version, Cursor: 100, RetryAfterSeconds: 5}); e != nil {
		t.Fatal(e)
	}
	now = now.Add(6 * time.Second)
	work, e = s.ClaimDeliveries(context.Background(), "worker-fixture", mobile.DeliveryClaim{Consumer: "nearby_alerts", Limit: 1})
	if e != nil || len(work) != 1 {
		t.Fatal(work, e)
	}
	lease := body[mobile.Delivery](t, work[0].Delivery)
	if lease.Cursor != 100 || lease.Attempts != 2 {
		t.Fatal(lease)
	}
	if e = s.CompleteDelivery(context.Background(), "worker-fixture", work[0].Delivery.ID, mobile.DeliveryResult{Consumer: "nearby_alerts", Version: work[0].Delivery.Version, Success: true, Cursor: 100, ResultRef: "invalid-partial"}); e == nil {
		t.Fatal("partial batch acknowledged")
	}
}

func TestReadReceiptReplayRechecksCurrentGroupMembership(t *testing.T) {
	s, repo := fixture(t)
	g := mobile.Group{Name: "fixture", Members: []int64{1, 2}}
	group := serverRecord(t, repo, mobile.Groups, mobile.Record{OwnerID: 1, State: "active"}, g)
	message := serverRecord(t, repo, mobile.GroupMessages, mobile.Record{OwnerID: 1, RefID: group.ID, State: "sent"}, mobile.RichContent{Text: "private group message"})
	ctx := mobile.WithCommandID(context.Background(), "group-read-receipt-fixture")
	first, e := s.ReadMessages(ctx, 2, group.ID, true, message.ID)
	if e != nil {
		t.Fatal(e)
	}
	again, e := s.ReadMessages(ctx, 2, group.ID, true, message.ID)
	if e != nil || first.ID != again.ID {
		t.Fatal("receipt replay rejected", e)
	}
	g.Members = []int64{1}
	serverRecord(t, repo, mobile.Groups, group, g)
	if _, e = s.ReadMessages(ctx, 2, group.ID, true, message.ID); !errors.Is(e, mobile.ErrForbidden) {
		t.Fatal("former member replayed private receipt", e)
	}
	if receipt, e := s.Operation(ctx, 2, "group-read-receipt-fixture"); e != nil || receipt.State != "committed" {
		t.Fatal("minimal command receipt disappeared", receipt, e)
	}
}

func TestNotificationReadDoesNotExposeProviderAcceptance(t *testing.T) {
	s, repo := fixture(t)
	n := mobile.Notification{Category: "interactions", PushState: "accepted", PushReceipts: []mobile.PushAcceptance{{Accepted: true, MessageID: "private-provider-ref"}}}
	r := serverRecord(t, repo, mobile.Notifications, mobile.Record{OwnerID: 1, State: "unread"}, n)
	result, e := s.ReadNotification(context.Background(), 1, r.ID, r.Version)
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Notification](t, result)
	if !v.Read || len(v.PushReceipts) != 0 {
		t.Fatal("provider receipt escaped through a write response", v)
	}
	if e = repo.View(context.Background(), func(tx mobile.Tx) error {
		r, e := tx.Get(mobile.Notifications, r.ID)
		if e != nil {
			return e
		}
		if len(body[mobile.Notification](t, r).PushReceipts) != 1 {
			t.Fatal("privacy projection destroyed stored acceptance")
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
}
