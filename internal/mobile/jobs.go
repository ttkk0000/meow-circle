package mobile

type JobPosition struct {
	Cursor int64  `json:"cursor"`
	After  string `json:"after,omitempty"`
}

type JobInput struct {
	Positions map[string]JobPosition `json:"positions"`
}

type JobResult struct {
	State             string                 `json:"state"`
	DeliveryID        int64                  `json:"delivery_id,omitempty"`
	Processed         int                    `json:"processed"`
	RetryAfterSeconds int                    `json:"retry_after_seconds,omitempty"`
	Positions         map[string]JobPosition `json:"positions"`
}
