package mobile_test

import (
	"context"
	"testing"
	"time"

	"kitty-circle/internal/mobile"
)

// Regression source only. No test execution is authorized for this task.
func TestTextAreaCaseDoesNotInventCoordinates(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	in := mobile.CaseInput{CommandMeta: meta(), Case: mobile.Case{
		Type: "lost", Title: "小满走失", Description: "仅提供城市和文字区域", City: " 武汉 ", Area: " 江岸区 ", Species: "cat", LocationPrecision: "area", OccurredAt: s.Now().Add(-time.Hour),
	}}
	r, e := s.SaveCase(ctx, 1, 0, in)
	if e != nil {
		t.Fatal(e)
	}
	c := body[mobile.Case](t, r)
	if c.LocationPrecision != "area" || c.Latitude != 0 || c.Longitude != 0 || c.City != "武汉" || c.Area != "江岸区" {
		t.Fatal("text area gained a fake location or lost its label", c)
	}
	in.CommandMeta = meta()
	in.Case.Latitude = 30.6
	in.Case.Longitude = 114.3
	if _, e = s.SaveCase(ctx, 1, 0, in); e == nil {
		t.Fatal("text area accepted a coordinate without approximate-point consent")
	}
	in.CommandMeta = meta()
	in.Case.LocationPrecision = "approximate"
	if _, e = s.SaveCase(ctx, 1, 0, in); e != nil {
		t.Fatal("coarse point no longer supported", e)
	}
}

func TestCitySubscriptionRequiresCityAndNoPoint(t *testing.T) {
	s, _ := fixture(t)
	ctx := context.Background()
	in := mobile.SubscriptionInput{Subscription: mobile.Subscription{LocationPrecision: "city", City: "武汉", Frequency: "daily", Enabled: true}}
	r, e := s.Subscribe(ctx, 1, in)
	if e != nil {
		t.Fatal(e)
	}
	v := body[mobile.Subscription](t, r)
	if v.Latitude != 0 || v.Longitude != 0 || v.RadiusKM != 0 || v.LocationPrecision != "city" {
		t.Fatal("city subscription acquired a synthetic point/radius", v)
	}
	in.Version = r.Version
	in.Subscription.Latitude = 30.6
	if _, e = s.Subscribe(ctx, 1, in); e == nil {
		t.Fatal("city subscription accepted coordinates")
	}
	in.Subscription.Latitude = 0
	in.Subscription.City = " "
	if _, e = s.Subscribe(ctx, 1, in); e == nil {
		t.Fatal("blank city subscription accepted")
	}
	legacy := mobile.SubscriptionInput{Subscription: mobile.Subscription{Latitude: 30.6, Longitude: 114.3, RadiusKM: 5, Frequency: "instant", Enabled: true}}
	if _, e = s.Subscribe(ctx, 2, legacy); e != nil {
		t.Fatal("legacy point subscription changed semantics", e)
	}
}
