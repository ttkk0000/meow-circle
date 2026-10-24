package mobileapi_test

import (
	"context"
	"encoding/json"
	"fmt"
	"testing"

	"kitty-circle/internal/mobile"
	"kitty-circle/internal/platform/mobileapi"
)

// Source regression only: the strict DTO must carry the key through HTTP.
func TestReviewSupplementPreservesRequestIdentityAcrossReplay(t *testing.T) {
	handler, token := apiFixture(t)
	h := handler.(*mobileapi.Handler)
	var review mobile.Record
	if e := h.Service.Repo.Update(context.Background(), func(tx mobile.Tx) error {
		review = mobile.Record{OwnerID: 1, State: "needs_info"}
		if e := mobile.SetBody(&review, mobile.Review{Type: "report", Reason: "original"}); e != nil {
			return e
		}
		return tx.Save(mobile.Reviews, &review)
	}); e != nil {
		t.Fatal(e)
	}
	body := fmt.Sprintf(`{"client_request_id":"review-supplement-fixture","version":%d,"note":"new evidence","media_ids":[]}`, review.Version)
	var firstVersion int64
	for i := 0; i < 2; i++ {
		w := request(t, h, "POST", fmt.Sprintf("/api/v2/reviews/%d/supplement", review.ID), token, body)
		if w.Code != 200 {
			t.Fatal(w.Code, w.Body.String())
		}
		var envelope struct {
			Data mobile.Record `json:"data"`
		}
		if e := json.Unmarshal(w.Body.Bytes(), &envelope); e != nil {
			t.Fatal(e)
		}
		if i == 0 {
			firstVersion = envelope.Data.Version
		} else if envelope.Data.Version != firstVersion {
			t.Fatal("replayed supplement changed the record twice")
		}
	}
	w := request(t, h, "GET", "/api/v2/operations/review-supplement-fixture", token, "")
	if w.Code != 200 {
		t.Fatal(w.Code, w.Body.String())
	}
}
