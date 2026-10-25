package mediablob

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

func TestS3PhysicalErasureRejectsVersionedBucketsAndDeleteMarkers(t *testing.T) {
	for _, status := range []string{"Enabled", "Suspended", ""} {
		t.Run("versioning_"+status, func(t *testing.T) {
			deletes := 0
			server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.Header.Get("Authorization") == "" || r.Header.Get("X-Amz-Content-Sha256") == "" {
					t.Error("unsigned private storage request")
				}
				if r.Method == http.MethodDelete {
					deletes++
					w.Header().Set("X-Amz-Delete-Marker", "true")
					w.WriteHeader(204)
					return
				}
				w.Header().Set("Content-Type", "application/xml")
				_, _ = w.Write([]byte("<VersioningConfiguration><Status>" + status + "</Status></VersioningConfiguration>"))
			}))
			defer server.Close()
			endpoint, _ := url.Parse(server.URL)
			storage := &S3{endpoint: endpoint, bucket: "fixture-private", prefix: "media/", region: "us-east-1", access: "fixture-access-key", secret: "fixture-secret-key", client: server.Client()}
			err := storage.Remove(context.Background(), "mnd-1-1-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg")
			if !errors.Is(err, ErrUnavailable) {
				t.Fatal("physical erasure was falsely confirmed", err)
			}
			if status != "" && deletes != 0 {
				t.Fatal("delete issued against versioned bucket")
			}
		})
	}
}

func TestS3RefusesUploadsWithoutVerifiedPublicAccessBlock(t *testing.T) {
	writes := 0
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPut {
			writes++
			w.WriteHeader(200)
			return
		}
		_, _ = w.Write([]byte("<PublicAccessBlockConfiguration><BlockPublicAcls>true</BlockPublicAcls><IgnorePublicAcls>true</IgnorePublicAcls><BlockPublicPolicy>false</BlockPublicPolicy><RestrictPublicBuckets>true</RestrictPublicBuckets></PublicAccessBlockConfiguration>"))
	}))
	defer server.Close()
	endpoint, _ := url.Parse(server.URL)
	storage := &S3{endpoint: endpoint, bucket: "fixture-private", prefix: "media/", region: "us-east-1", access: "fixture-access-key", secret: "fixture-secret-key", client: server.Client()}
	if err := storage.Put(context.Background(), "mnd-1-1-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg", []byte("fixture")); !errors.Is(err, ErrUnavailable) {
		t.Fatal("unverified public access was accepted", err)
	}
	if writes != 0 {
		t.Fatal("upload reached an unverified bucket")
	}
}
