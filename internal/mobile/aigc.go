package mobile

import "encoding/json"

// GB 45438-2025 Appendix E. Only this allowlist survives privacy re-encoding;
// camera, location and unrelated source metadata are never retained.
type AIGCFields struct {
	Label             string `json:"Label"`
	ContentProducer   string `json:"ContentProducer"`
	ProduceID         string `json:"ProduceID"`
	ReservedCode1     string `json:"ReservedCode1"`
	ContentPropagator string `json:"ContentPropagator"`
	PropagateID       string `json:"PropagateID"`
	ReservedCode2     string `json:"ReservedCode2"`
}
type AIGCLabel struct {
	AIGC AIGCFields `json:"AIGC"`
}

func (v AIGCLabel) Valid() bool {
	if !HasString([]string{"1", "2", "3"}, v.AIGC.Label) {
		return false
	}
	for _, s := range []string{v.AIGC.ContentProducer, v.AIGC.ProduceID, v.AIGC.ReservedCode1, v.AIGC.ContentPropagator, v.AIGC.PropagateID, v.AIGC.ReservedCode2} {
		if len(s) > 1024 {
			return false
		}
		for _, c := range s {
			if c < 0x21 || c > 0x7e || c == '\\' {
				return false
			}
		}
	}
	return true
}
func PropagationLabel(source, propagator, id, status string) (string, error) {
	if propagator == "" {
		return "", ErrUnavailable
	}
	v := AIGCLabel{AIGC: AIGCFields{Label: status}}
	if source != "" {
		if e := json.Unmarshal([]byte(source), &v); e != nil {
			return "", e
		}
	}
	v.AIGC.ContentPropagator, v.AIGC.PropagateID, v.AIGC.ReservedCode2 = propagator, id, ""
	if !v.Valid() {
		return "", Invalid("invalid AIGC metadata")
	}
	b, e := json.Marshal(v)
	return string(b), e
}
