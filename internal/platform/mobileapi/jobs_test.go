package mobileapi_test

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/mobileapi"
)

// Source only; no HTTP server, filesystem fixture or application test was run.
func jobRequest(h *mobileapi.Handler, job, token, body string) *httptest.ResponseRecorder {
	r := httptest.NewRequest(http.MethodPost, "/api/v2/ops/jobs/"+job, strings.NewReader(body))
	r.Header.Set("X-Operator-Token", token)
	w := httptest.NewRecorder()
	h.ServeHTTP(w, r)
	return w
}

func TestJobsEnforceNamedConsumerAndPrivacyRoles(t *testing.T) {
	handler, _ := apiFixture(t)
	h := handler.(*mobileapi.Handler)
	worker, privacy := strings.Repeat("w", 32), strings.Repeat("p", 32)
	h.Operators = []mobileapi.Operator{
		{ID: "push-fixture", Token: worker, Roles: []string{"worker"}, Consumers: []string{"push", "privacy_cleanup"}},
		{ID: "privacy-fixture", Token: privacy, Roles: []string{"privacy"}},
	}
	for _, tc := range []struct {
		job, token, body string
		want             int
	}{
		{"push", worker, `{}`, 200},
		{"nearby_alerts", worker, `{}`, 403},
		{"privacy_cleanup", worker, `{}`, 403},
		{"maintenance", worker, `{}`, 403},
		{"push", privacy, `{}`, 403},
		{"push", worker, `{"positions":{"uploads":{"cursor":0}}}`, 400},
		{"maintenance", privacy, `{"positions":{"evidence":{"cursor":-1}}}`, 400},
		{"maintenance", privacy, `{"positions":{"tokens":{"after":"unexpected"}}}`, 400},
		{"unknown", worker, `{}`, 404},
	} {
		if w := jobRequest(h, tc.job, tc.token, tc.body); w.Code != tc.want {
			t.Fatalf("%s: %d, want %d", tc.job, w.Code, tc.want)
		}
	}
}

func TestPrivacyJobAcknowledgesAfterPhysicalFileAndProgressReceipt(t *testing.T) {
	handler, _ := apiFixture(t)
	h := handler.(*mobileapi.Handler)
	token := strings.Repeat("p", 32)
	h.Operators = []mobileapi.Operator{{ID: "privacy-fixture", Token: token, Roles: []string{"worker", "privacy"}, Consumers: []string{"privacy_cleanup"}}}
	filename := "fixture-private-photo.png"
	if e := os.WriteFile(filepath.Join(h.MediaDir, filename), []byte("fixture"), 0600); e != nil {
		t.Fatal(e)
	}
	var request mobile.Record
	if e := h.Service.Repo.Update(context.Background(), func(tx mobile.Tx) error {
		photo := mobile.Record{OwnerID: 1, State: "ready"}
		if e := mobile.SetBody(&photo, mobile.MediaFile{Scope: "private", Filename: filename}); e != nil {
			return e
		}
		if e := tx.Save(mobile.Media, &photo); e != nil {
			return e
		}
		request = mobile.Record{OwnerID: 1, State: "processing"}
		if e := mobile.SetBody(&request, mobile.DataRequest{Type: "erase_fields", ExternalState: "not_required", Note: "never-in-worker-response", QueryHash: "never-in-worker-response", Items: []mobile.DataItem{{Kind: mobile.Media, ID: photo.ID, State: "pending"}}}); e != nil {
			return e
		}
		if e := tx.Save(mobile.DataRequests, &request); e != nil {
			return e
		}
		event := mobile.Record{State: "pending"}
		if e := mobile.SetBody(&event, mobile.Event{SchemaVersion: 1, Topic: "data_request.submitted", AggregateKind: mobile.DataRequests, AggregateID: request.ID}); e != nil {
			return e
		}
		return tx.Save(mobile.Outbox, &event)
	}); e != nil {
		t.Fatal(e)
	}
	w := jobRequest(h, "privacy_cleanup", token, `{}`)
	var envelope struct {
		OK   bool             `json:"ok"`
		Data mobile.JobResult `json:"data"`
	}
	if e := json.Unmarshal(w.Body.Bytes(), &envelope); e != nil || w.Code != 200 || !envelope.OK || envelope.Data.State != "acknowledged" || envelope.Data.Processed != 1 {
		t.Fatal(w.Code, w.Body.String(), e)
	}
	if _, e := os.Stat(filepath.Join(h.MediaDir, filename)); !errors.Is(e, os.ErrNotExist) {
		t.Fatal("physical copy remains", e)
	}
	if strings.Contains(w.Body.String(), "never-in-worker-response") {
		t.Fatal("private request material leaked")
	}
	w = jobRequest(h, "privacy_cleanup", token, `{}`)
	if e := json.Unmarshal(w.Body.Bytes(), &envelope); e != nil || envelope.Data.State != "idle" {
		t.Fatal("progress caused another cleanup loop", w.Body.String(), e)
	}
}

func TestSupersededNotificationJobTerminates(t *testing.T) {
	handler, _ := apiFixture(t)
	h := handler.(*mobileapi.Handler)
	token := strings.Repeat("w", 32)
	h.Operators = []mobileapi.Operator{{ID: "push-fixture", Token: token, Roles: []string{"worker"}, Consumers: []string{"push"}}}
	if e := h.Service.Repo.Update(context.Background(), func(tx mobile.Tx) error {
		event := mobile.Record{State: "pending"}
		if e := mobile.SetBody(&event, mobile.Event{SchemaVersion: 1, Topic: "notification.created", AggregateKind: mobile.Notifications, AggregateID: 999}); e != nil {
			return e
		}
		return tx.Save(mobile.Outbox, &event)
	}); e != nil {
		t.Fatal(e)
	}
	w := jobRequest(h, "push", token, `{}`)
	if w.Code != 200 || !strings.Contains(w.Body.String(), `"state":"acknowledged"`) {
		t.Fatal(w.Code, w.Body.String())
	}
}
