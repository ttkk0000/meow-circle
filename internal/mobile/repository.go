// Package mobile contains the phone application's transactional business modules.
// It has no HTTP, SQL, Redis or SDK dependencies.
package mobile

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"kitty-circle/internal/domain"
)

type Kind string

const (
	Accounts         Kind = "accounts"
	Sessions         Kind = "sessions"
	Identities       Kind = "identities"
	AuthChallenges   Kind = "auth_challenges"
	OTPChallenges    Kind = "otp_challenges"
	Friendships      Kind = "friendships"
	Pets             Kind = "pets"
	Offers           Kind = "offers"
	Entries          Kind = "entries"
	Comments         Kind = "entry_comments"
	PetComments      Kind = "pet_comments"
	CommentReactions Kind = "comment_reactions"
	Reactions        Kind = "entry_reactions"
	Cases            Kind = "cases"
	Claims           Kind = "claims"
	Leads            Kind = "leads"
	Subscriptions    Kind = "subscriptions"
	Appointments     Kind = "appointments"
	Adoptions        Kind = "adoptions"
	CareUpdates      Kind = "care_updates"
	Groups           Kind = "groups"
	GroupMessages    Kind = "group_messages"
	Conversations    Kind = "conversations"
	Messages         Kind = "messages"
	Receipts         Kind = "receipts"
	Invitations      Kind = "invitations"
	Notifications    Kind = "notifications"
	Preferences      Kind = "preferences"
	Expressions      Kind = "expressions"
	Drafts           Kind = "drafts"
	Media            Kind = "media"
	Reviews          Kind = "reviews"
	VisitFeedback    Kind = "visit_feedback"
	Outbox           Kind = "outbox"
	Qualifications   Kind = "qualifications"
	Guardians        Kind = "guardians"
	Operations       Kind = "operations"
	Deliveries       Kind = "deliveries"
	DataRequests     Kind = "data_requests"
	RightsCases      Kind = "rights_cases"
	Authorities      Kind = "authorities"
	AuditEntries     Kind = "audit_entries"
	PushDevices      Kind = "push_devices"
	RevenueLedger    Kind = "revenue_ledger"
)

var Kinds = []Kind{Accounts, Sessions, Identities, AuthChallenges, OTPChallenges,
	Friendships, Pets, Offers, Entries, Comments, PetComments, CommentReactions, Reactions, Cases, Claims, Leads,
	Subscriptions, Appointments, Adoptions, CareUpdates, Groups, GroupMessages,
	Conversations, Messages, Receipts, Invitations, Notifications, Preferences, Expressions, Drafts, Media, Reviews, VisitFeedback, Outbox,
	Qualifications, Guardians, Operations, Deliveries, DataRequests, RightsCases, Authorities, AuditEntries, PushDevices, RevenueLedger}

func (k Kind) Valid() bool {
	for _, allowed := range Kinds {
		if k == allowed {
			return true
		}
	}
	return false
}

// Record is an infrastructure envelope, never an HTTP request DTO. Each Kind
// has its own table and typed body. Relational headers support ownership,
// participant, parent, media-reference and optimistic concurrency queries.
type Record struct {
	ID           int64           `json:"id"`
	OwnerID      int64           `json:"owner_id"`
	Participants []int64         `json:"-"`
	Key          string          `json:"-"`
	RefID        int64           `json:"reference_id,omitempty"`
	MediaIDs     []int64         `json:"media_ids,omitempty"`
	Public       bool            `json:"-"`
	State        string          `json:"state"`
	Version      int64           `json:"version"`
	CreatedAt    time.Time       `json:"created_at"`
	UpdatedAt    time.Time       `json:"updated_at"`
	Data         json.RawMessage `json:"data"`
}

type Filter struct {
	KeyPrefix     string
	OwnerID       int64
	ParticipantID int64
	RefID         int64
	MediaID       int64
	PublicOnly    bool
	After         int64
	Limit         int
	State         string
}

// Tx implementations must isolate a callback atomically, roll back on error,
// and return storage failures. Save uses the existing record version (0=create)
// and increments it only after a successful write.
type Tx interface {
	Get(Kind, int64) (Record, error)
	ByKey(Kind, string) (Record, error)
	List(Kind, Filter) ([]Record, error)
	Count(Kind, Filter) (int64, error)
	Save(Kind, *Record) error
	Delete(Kind, int64, int64) error
	User(int64) (domain.User, error)
	UserByPhone(string) (domain.User, error)
	UserByName(string) (domain.User, error)
	SaveUser(*domain.User) error
}

type Repository interface {
	View(context.Context, func(Tx) error) error
	Update(context.Context, func(Tx) error) error
}

var (
	ErrNotFound     = errors.New("not found")
	ErrConflict     = errors.New("version or unique key conflict")
	ErrForbidden    = errors.New("permission denied")
	ErrUnavailable  = errors.New("service not configured or unavailable")
	ErrUnauthorized = errors.New("authentication required")
	ErrRateLimited  = errors.New("too many attempts")
)

type ValidationError struct{ Message string }

func (e ValidationError) Error() string        { return e.Message }
func Invalid(format string, args ...any) error { return ValidationError{fmt.Sprintf(format, args...)} }

func Decode[T any](r Record) (T, error) {
	var value T
	err := json.Unmarshal(r.Data, &value)
	return value, err
}

func SetBody(r *Record, value any) error {
	data, err := json.Marshal(value)
	if err == nil {
		r.Data = data
	}
	return err
}

func Has(ids []int64, id int64) bool {
	for _, candidate := range ids {
		if candidate == id {
			return true
		}
	}
	return false
}

func Unique(ids []int64) []int64 {
	result := make([]int64, 0, len(ids))
	for _, id := range ids {
		if id > 0 && !Has(result, id) {
			result = append(result, id)
		}
	}
	return result
}
