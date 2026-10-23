package mobile

import "context"

type commandContextKey struct{}

func WithCommandID(ctx context.Context, key string) context.Context {
	return context.WithValue(ctx, commandContextKey{}, key)
}
func CommandID(ctx context.Context) string {
	key, _ := ctx.Value(commandContextKey{}).(string)
	return key
}

func CommandContext(ctx context.Context, key string) (context.Context, error) {
	if key == "" {
		return ctx, nil
	}
	if current := CommandID(ctx); current != "" && current != key {
		return ctx, Denied("idempotency_conflict")
	}
	if _, _, e := CommandIdentity(CommandMeta{ClientRequestID: key}); e != nil {
		return ctx, e
	}
	return WithCommandID(ctx, key), nil
}

func (s *Service) checkActor(ctx context.Context, tx Tx, actor int64) error {
	if e := s.active(tx, actor); e != nil {
		return e
	}
	if key := SessionKey(ctx); key != "" {
		return CheckSession(tx, actor, key, s.Now())
	}
	return nil
}

func (s *Service) actorUpdate(ctx context.Context, actor int64, fn func(Tx) error) error {
	return s.Repo.Update(ctx, func(tx Tx) error {
		if e := s.checkActor(ctx, tx, actor); e != nil {
			return e
		}
		return fn(tx)
	})
}

// Existing desired-state/versioned endpoints keep their wire DTOs. Callers can
// opt into a durable operation receipt using the same Idempotency-Key header.
func (s *Service) mutate(ctx context.Context, actor int64, name string, object int64, input any, kind Kind, out *Record, fn func(Tx) error) error {
	if key := CommandID(ctx); key != "" {
		return s.command(ctx, actor, name, object, struct {
			CommandMeta
			Payload any `json:"payload"`
		}{CommandMeta{ClientRequestID: key}, input}, kind, out, fn)
	}
	return s.actorUpdate(ctx, actor, fn)
}
