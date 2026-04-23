package domain

import "time"

type User struct {
	ID           int64     `json:"id"`
	Username     string    `json:"username"`
	Nickname     string    `json:"nickname"`
	Phone        string    `json:"-"` // stored normalized digits-only; never exposed in JSON
	AvatarURL    string    `json:"avatar_url,omitempty"`
	Bio          string    `json:"bio,omitempty"`
	PasswordHash string    `json:"-"`
	PasswordSalt string    `json:"-"`
	CreatedAt    time.Time `json:"created_at"`
}

type PostCategory string

const (
	CategoryDailyShare PostCategory = "daily_share"
	CategoryHelp       PostCategory = "help"
	CategoryActivity   PostCategory = "activity"
	CategoryTrade      PostCategory = "trade"
)

type Post struct {
	ID          int64        `json:"id"`
	AuthorID    int64        `json:"author_id"`
	Title       string       `json:"title"`
	Content     string       `json:"content"`
	Category    PostCategory `json:"category"`
	Tags        []string     `json:"tags"`
	MediaIDs    []int64      `json:"media_ids,omitempty"`
	CreatedAt   time.Time    `json:"created_at"`
	LastReplyAt time.Time    `json:"last_reply_at"`
}

type Comment struct {
	ID        int64     `json:"id"`
	PostID    int64     `json:"post_id"`
	AuthorID  int64     `json:"author_id"`
	Content   string    `json:"content"`
	CreatedAt time.Time `json:"created_at"`
}

type ListingType string

const (
	ListingTypeProduct ListingType = "product"
	ListingTypeService ListingType = "service"
	ListingTypeAdopt   ListingType = "adopt"
)

type Listing struct {
	ID          int64       `json:"id"`
	SellerID    int64       `json:"seller_id"`
	Type        ListingType `json:"type"`
	Title       string      `json:"title"`
	Description string      `json:"description"`
	PriceCents  int64       `json:"price_cents"`
	Currency    string      `json:"currency"`
	MediaIDs    []int64     `json:"media_ids,omitempty"`
	CreatedAt   time.Time   `json:"created_at"`
}

// ===== Media =====

type MediaKind string

const (
	MediaKindImage MediaKind = "image"
	MediaKindVideo MediaKind = "video"
)

type MediaStatus string

const (
	MediaStatusApproved MediaStatus = "approved"
	MediaStatusPending  MediaStatus = "pending"
	MediaStatusRejected MediaStatus = "rejected"
)

type Media struct {
	ID        int64       `json:"id"`
	OwnerID   int64       `json:"owner_id"`
	Kind      MediaKind   `json:"kind"`
	MIME      string      `json:"mime"`
	Size      int64       `json:"size"`
	Filename  string      `json:"filename"`
	URL       string      `json:"url"`
	Status    MediaStatus `json:"status"`
	CreatedAt time.Time   `json:"created_at"`
}

// ===== Report / Audit =====

type ReportTargetKind string

const (
	ReportTargetPost    ReportTargetKind = "post"
	ReportTargetComment ReportTargetKind = "comment"
	ReportTargetListing ReportTargetKind = "listing"
	ReportTargetMedia   ReportTargetKind = "media"
)

type ReportStatus string

const (
	ReportStatusOpen      ReportStatus = "open"
	ReportStatusResolved  ReportStatus = "resolved"
	ReportStatusDismissed ReportStatus = "dismissed"
)

type Report struct {
	ID         int64            `json:"id"`
	ReporterID int64            `json:"reporter_id"`
	TargetKind ReportTargetKind `json:"target_kind"`
	TargetID   int64            `json:"target_id"`
	Reason     string           `json:"reason"`
	Status     ReportStatus     `json:"status"`
	HandledBy  string           `json:"handled_by,omitempty"`
	Resolution string           `json:"resolution,omitempty"`
	CreatedAt  time.Time        `json:"created_at"`
	UpdatedAt  time.Time        `json:"updated_at"`
}

// ===== Order / Payment =====

type OrderStatus string

const (
	OrderStatusPendingPayment OrderStatus = "pending_payment"
	OrderStatusPaid           OrderStatus = "paid"
	OrderStatusShipped        OrderStatus = "shipped"
	OrderStatusCompleted      OrderStatus = "completed"
	OrderStatusCancelled      OrderStatus = "cancelled"
	OrderStatusRefunded       OrderStatus = "refunded"
)

type PaymentMethod string

const (
	PaymentMethodMock   PaymentMethod = "mock"
	PaymentMethodAlipay PaymentMethod = "alipay"
	PaymentMethodWechat PaymentMethod = "wechat"
	PaymentMethodStripe PaymentMethod = "stripe"
)

type Order struct {
	ID            int64         `json:"id"`
	BuyerID       int64         `json:"buyer_id"`
	SellerID      int64         `json:"seller_id"`
	ListingID     int64         `json:"listing_id"`
	ListingTitle  string        `json:"listing_title"`
	AmountCents   int64         `json:"amount_cents"`
	Currency      string        `json:"currency"`
	Status        OrderStatus   `json:"status"`
	PaymentMethod PaymentMethod `json:"payment_method,omitempty"`
	PaymentTxID   string        `json:"payment_tx_id,omitempty"`
	Note          string        `json:"note,omitempty"`
	CreatedAt     time.Time     `json:"created_at"`
	UpdatedAt     time.Time     `json:"updated_at"`
	PaidAt        *time.Time    `json:"paid_at,omitempty"`
	ShippedAt     *time.Time    `json:"shipped_at,omitempty"`
	CompletedAt   *time.Time    `json:"completed_at,omitempty"`
}

// ===== Notifications =====

type NotificationKind string

const (
	NotificationComment       NotificationKind = "comment"
	NotificationOrder         NotificationKind = "order"
	NotificationReportHandled NotificationKind = "report_handled"
	NotificationSystem        NotificationKind = "system"
	NotificationMessage       NotificationKind = "message"
)

type Notification struct {
	ID        int64            `json:"id"`
	UserID    int64            `json:"user_id"`
	Kind      NotificationKind `json:"kind"`
	Title     string           `json:"title"`
	Body      string           `json:"body,omitempty"`
	RefID     int64            `json:"ref_id,omitempty"`
	Read      bool             `json:"read"`
	CreatedAt time.Time        `json:"created_at"`
}

// ===== Messages =====

type Message struct {
	ID          int64     `json:"id"`
	SenderID    int64     `json:"sender_id"`
	RecipientID int64     `json:"recipient_id"`
	Content     string    `json:"content"`
	Read        bool      `json:"read"`
	CreatedAt   time.Time `json:"created_at"`
}

type Conversation struct {
	Peer         User      `json:"peer"`
	LastMessage  string    `json:"last_message"`
	LastSenderID int64     `json:"last_sender_id"`
	UnreadCount  int       `json:"unread_count"`
	UpdatedAt    time.Time `json:"updated_at"`
}

// ===== Audit Log =====

type AuditLog struct {
	ID         int64     `json:"id"`
	Actor      string    `json:"actor"`
	Action     string    `json:"action"`
	TargetKind string    `json:"target_kind,omitempty"`
	TargetID   int64     `json:"target_id,omitempty"`
	Note       string    `json:"note,omitempty"`
	IP         string    `json:"ip,omitempty"`
	CreatedAt  time.Time `json:"created_at"`
}
