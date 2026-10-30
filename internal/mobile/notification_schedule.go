package mobile

import "time"

var notificationDigestZone = time.FixedZone("Asia/Shanghai", 8*60*60)

// Notification preferences promise the next 19:00 in China Standard Time.
// Persist UTC so delivery does not depend on the worker host's timezone.
func nextNotificationDigest(now time.Time) time.Time {
	local := now.In(notificationDigestZone)
	next := time.Date(local.Year(), local.Month(), local.Day(), 19, 0, 0, 0, notificationDigestZone)
	if !next.After(now) {
		next = next.AddDate(0, 0, 1)
	}
	return next.UTC()
}
