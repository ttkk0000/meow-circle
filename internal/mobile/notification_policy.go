package mobile

import (
	"fmt"
	"strings"
)

// Apply the current inbox policy without rewriting historical IDs, read state or delivery receipts.
func notificationCategory(record Record, body Notification) string {
	if body.Category == "interactions" && (body.TargetKind == Friendships ||
		body.TargetKind == Cases && strings.HasPrefix(record.Key, fmt.Sprintf("case-watch:%d:", body.TargetID))) {
		return "applications"
	}
	return body.Category
}
