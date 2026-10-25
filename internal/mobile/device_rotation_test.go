package mobile_test

import (
	"context"
	"testing"

	"kitty-circle/internal/mobile"
)

func TestPushTokenRotationKeepsOtherSessionsAndRecoversVersion(t *testing.T) {
	s, repo := fixture(t)
	firstCtx, _ := sessionAndGrant(t, repo, 1, mobile.PurposeScope{})
	secondCtx, _ := sessionAndGrant(t, repo, 1, mobile.PurposeScope{})
	first, err := s.RegisterPushDevice(firstCtx, 1, mobile.PushDeviceInput{Platform: "ios", Token: "fixture-first-device-token", Permission: "granted", Enabled: true})
	if err != nil {
		t.Fatal(err)
	}
	second, err := s.RegisterPushDevice(secondCtx, 1, mobile.PushDeviceInput{Platform: "ios", Token: "fixture-second-device-token", Permission: "granted", Enabled: true})
	if err != nil {
		t.Fatal(err)
	}
	rotated, err := s.RegisterPushDevice(firstCtx, 1, mobile.PushDeviceInput{Platform: "ios", Token: "fixture-rotated-device-token", Permission: "granted", Enabled: true})
	if err != nil {
		t.Fatal(err)
	}
	lookup, err := s.PushDeviceStatus(firstCtx, 1, mobile.PushDeviceLookup{Platform: "ios", Token: "fixture-rotated-device-token"})
	if err != nil || lookup != rotated {
		t.Fatal("registration result could not be reconciled", lookup, err)
	}
	if err = repo.View(context.Background(), func(tx mobile.Tx) error {
		old, e := tx.Get(mobile.PushDevices, first.ID)
		if e != nil {
			return e
		}
		other, e := tx.Get(mobile.PushDevices, second.ID)
		if e != nil {
			return e
		}
		if old.State != "disabled" || other.State != "enabled" {
			t.Fatal("rotation changed another device", old.State, other.State)
		}
		return nil
	}); err != nil {
		t.Fatal(err)
	}
}

func TestPushWithdrawalWithoutTokenDisablesOnlyCurrentSession(t *testing.T) {
	s, repo := fixture(t)
	current, _ := sessionAndGrant(t, repo, 1, mobile.PurposeScope{})
	other, _ := sessionAndGrant(t, repo, 1, mobile.PurposeScope{})
	first, e := s.RegisterPushDevice(current, 1, mobile.PushDeviceInput{Platform: "android", Token: "fixture-current-session-token", Permission: "granted", Enabled: true})
	if e != nil {
		t.Fatal(e)
	}
	second, e := s.RegisterPushDevice(other, 1, mobile.PushDeviceInput{Platform: "android", Token: "fixture-other-session-token", Permission: "granted", Enabled: true})
	if e != nil {
		t.Fatal(e)
	}
	if e = s.DisableCurrentPush(current, 1); e != nil {
		t.Fatal(e)
	}
	if e = repo.View(context.Background(), func(tx mobile.Tx) error {
		a, e := tx.Get(mobile.PushDevices, first.ID)
		if e != nil {
			return e
		}
		b, e := tx.Get(mobile.PushDevices, second.ID)
		if e != nil {
			return e
		}
		if a.State != "disabled" || b.State != "enabled" {
			t.Fatal("session withdrawal crossed device boundary")
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
	if _, e = s.Settings(current, 1, mobile.Preferences, mobile.SettingsInput{Preference: &mobile.Preference{Frequency: "instant", PushPermission: false}}); e != nil {
		t.Fatal(e)
	}
	if e = repo.View(context.Background(), func(tx mobile.Tx) error {
		rows, e := tx.List(mobile.PushDevices, mobile.Filter{OwnerID: 1, State: "enabled"})
		if e != nil {
			return e
		}
		if len(rows) != 0 {
			t.Fatal("account withdrawal retained active devices")
		}
		return nil
	}); e != nil {
		t.Fatal(e)
	}
}
