package mobile

import "context"

type DataRequestWork struct {
	ID              int64      `json:"id"`
	Version         int64      `json:"version"`
	State           string     `json:"state"`
	Type            string     `json:"type"`
	ProgressVersion int        `json:"progress_version"`
	ExternalState   string     `json:"external_state"`
	Items           []DataItem `json:"items"`
}

func (s *Service) DataRequestWork(ctx context.Context, operator string, id int64) (out DataRequestWork, err error) {
	if operator == "" {
		return out, ErrForbidden
	}
	err = s.Repo.Update(ctx, func(tx Tx) error {
		r, e := tx.Get(DataRequests, id)
		if e != nil {
			return e
		}
		d, e := Decode[DataRequest](r)
		if e != nil {
			return e
		}
		out = DataRequestWork{r.ID, r.Version, r.State, d.Type, d.ProgressVersion, d.ExternalState, append([]DataItem{}, d.Items...)}
		return audit(tx, operator, "data_request.worker_read", DataRequests, id)
	})
	return
}
