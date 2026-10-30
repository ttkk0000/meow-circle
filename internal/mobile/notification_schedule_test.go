package mobile

import (
	"testing"
	"time"
)

func TestNotificationDigestChinaTimeBoundary(t *testing.T) {
	for _, tc := range []struct{ name, now, want string }{
		{"before_cutoff", "2026-10-29T18:59:59+08:00", "2026-10-29T11:00:00Z"},
		{"at_cutoff", "2026-10-29T19:00:00+08:00", "2026-10-30T11:00:00Z"},
		{"china_day_differs_from_utc", "2026-10-28T23:30:00Z", "2026-10-29T11:00:00Z"},
		{"year_rollover", "2026-12-31T23:59:59+08:00", "2027-01-01T11:00:00Z"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			now, err := time.Parse(time.RFC3339, tc.now)
			if err != nil {
				t.Fatal(err)
			}
			if got := nextNotificationDigest(now).Format(time.RFC3339); got != tc.want {
				t.Fatalf("digest time = %s, want %s", got, tc.want)
			}
		})
	}
}
