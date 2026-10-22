package mobile_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// 仅补回归源码；本任务未获测试执行授权。
func TestExpiredUploadRetryUsesNewFileAndRejectsOldCompletion(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	now := time.Now().UTC()
	s.Now = func() time.Time { return now }
	file := mobile.MediaFile{Scope: "public", MIME: "image/png", SHA256: mobile.Fingerprint("same photo")}
	first, e := s.ReserveMedia(ctx, 1, "", "original-upload", file)
	if e != nil {
		t.Fatal(e)
	}
	before, _ := mobile.Decode[mobile.MediaFile](first)
	now = now.Add(3 * time.Minute)
	second, e := s.ReserveMedia(ctx, 1, "", "original-upload", file)
	if e != nil {
		t.Fatal(e)
	}
	after, _ := mobile.Decode[mobile.MediaFile](second)
	if second.ID != first.ID || before.Filename == after.Filename {
		t.Fatal("retry did not retain logical ID and rotate physical generation")
	}
	if _, e = s.CompleteReservedMedia(ctx, 1, "", first.ID, first.Version); !errors.Is(e, mobile.ErrConflict) {
		t.Fatal("stale writer completed", e)
	}
	if _, e = s.CompleteReservedMedia(ctx, 1, "", second.ID, second.Version); e != nil {
		t.Fatal(e)
	}
	used, e := s.UploadFilesInUse(ctx, []string{before.Filename, after.Filename})
	if e != nil || used[before.Filename] != "" || used[after.Filename] != "ready" {
		t.Fatal(used, e)
	}
	if _, e = s.ReserveMedia(ctx, 1, "", "original-upload", mobile.MediaFile{Scope: "public", MIME: "image/png", SHA256: mobile.Fingerprint("changed")}); e == nil {
		t.Fatal("same key accepted different bytes")
	}
}

func TestUploadExpirySkipsLiveLeaseAndScopedHold(t *testing.T) {
	s, repo := fixture(t)
	ctx := context.Background()
	now := time.Now().UTC()
	s.Now = func() time.Time { return now }
	file := mobile.MediaFile{Scope: "public", MIME: "image/png", SHA256: mobile.Fingerprint("photo")}
	expired, e := s.ReserveMedia(ctx, 1, "", "expired-upload", file)
	if e != nil {
		t.Fatal(e)
	}
	held, e := s.ReserveMedia(ctx, 1, "", "retained-upload", file)
	if e != nil {
		t.Fatal(e)
	}
	request := mobile.Record{OwnerID: 1, State: "processing"}
	if e = repo.Update(ctx, func(tx mobile.Tx) error {
		if e := mobile.SetBody(&request, mobile.DataRequest{Holds: []mobile.DataHold{{Kind: mobile.Media, ID: held.ID, Basis: "fixture", Responsible: "reviewer", ReviewAt: now.Add(time.Hour)}}}); e != nil {
			return e
		}
		return tx.Save(mobile.DataRequests, &request)
	}); e != nil {
		t.Fatal(e)
	}
	now = now.Add(3 * time.Minute)
	live, e := s.ReserveMedia(ctx, 1, "", "current-upload", file)
	if e != nil {
		t.Fatal(e)
	}
	_, count, e := s.ExpireMediaUploads(ctx, "privacy-worker", 0)
	if e != nil || count != 1 {
		t.Fatal(count, e)
	}
	if e = repo.View(ctx, func(tx mobile.Tx) error {
		for id, state := range map[int64]string{expired.ID: "upload_expired", held.ID: "uploading", live.ID: "uploading"} {
			r, e := tx.Get(mobile.Media, id)
			if e != nil {
				return e
			}
			if r.State != state {
				t.Fatalf("%d: %s", id, r.State)
			}
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
	if e = s.SetDataHold(ctx, "privacy-worker", request.ID, mobile.DataHoldInput{Version: request.Version,
		Hold: mobile.DataHold{Kind: mobile.Media, ID: expired.ID, Basis: "late", Responsible: "reviewer", ReviewAt: now.Add(time.Hour)}}); e == nil {
		t.Fatal("accepted retention after reclamation decision")
	}
}
