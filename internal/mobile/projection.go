package mobile

import "context"

// 外部投影只接失效事件与最小审计元数据，不复制正文、私信、材料、令牌或精确位置。
// 搜索接收端只返回候选ID；读取仍须经过当前服务端权限检查。
type ProjectionEvent struct {
	Consumer string `json:"consumer"`
	EventID  int64  `json:"event_id"`
	Event    Event  `json:"event"`
}
type ProjectionAcceptance struct {
	Consumer string `json:"consumer"`
	EventID  int64  `json:"event_id"`
	Accepted bool   `json:"accepted"`
}
type ProjectionSink interface {
	Publish(context.Context, ProjectionEvent) (ProjectionAcceptance, error)
}

func (s *Service) DispatchProjection(ctx context.Context, consumer string, envelope EventEnvelope) error {
	if !HasString([]string{"search_projection", "audit_projection"}, consumer) || !consumerEvent(consumer, envelope.Event) || envelope.Event.SchemaVersion != 1 {
		return ErrForbidden
	}
	sink := s.SearchProjection
	if consumer == "audit_projection" {
		sink = s.AuditProjection
	}
	if sink == nil {
		return ErrUnavailable
	}
	event := envelope.Event
	if consumer == "search_projection" {
		event.ActorID = 0
	}
	receipt, err := sink.Publish(ctx, ProjectionEvent{Consumer: consumer, EventID: envelope.ID, Event: event})
	if err != nil || !receipt.Accepted || receipt.Consumer != consumer || receipt.EventID != envelope.ID {
		return ErrUnavailable
	}
	return nil
}
