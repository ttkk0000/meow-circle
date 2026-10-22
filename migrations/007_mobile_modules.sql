-- Phone domain aggregates. Apply after 001-006, before deploying the v2 API.
-- No demo data, SMS codes, provider credentials or user identity is seeded.
BEGIN;
CREATE SEQUENCE IF NOT EXISTS mobile_id_seq;
CREATE TABLE IF NOT EXISTS mobile_accounts (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_accounts_key ON mobile_accounts(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_accounts_owner ON mobile_accounts(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_accounts_ref ON mobile_accounts(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_sessions (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_sessions_key ON mobile_sessions(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_sessions_owner ON mobile_sessions(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_sessions_ref ON mobile_sessions(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_identities (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_identities_key ON mobile_identities(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_identities_owner ON mobile_identities(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_identities_ref ON mobile_identities(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_auth_challenges (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_auth_challenges_key ON mobile_auth_challenges(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_auth_challenges_owner ON mobile_auth_challenges(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_auth_challenges_ref ON mobile_auth_challenges(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_otp_challenges (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_otp_challenges_key ON mobile_otp_challenges(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_otp_challenges_owner ON mobile_otp_challenges(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_otp_challenges_ref ON mobile_otp_challenges(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_friendships (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_friendships_key ON mobile_friendships(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_friendships_owner ON mobile_friendships(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_friendships_ref ON mobile_friendships(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_friendships_participants ON mobile_friendships USING gin(participants);

CREATE TABLE IF NOT EXISTS mobile_pets (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_pets_key ON mobile_pets(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_pets_owner ON mobile_pets(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_pets_ref ON mobile_pets(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_pets_media ON mobile_pets USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_entries (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_entries_key ON mobile_entries(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_entries_owner ON mobile_entries(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_entries_ref ON mobile_entries(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_entries_media ON mobile_entries USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_entry_comments (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_entry_comments_key ON mobile_entry_comments(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_entry_comments_owner ON mobile_entry_comments(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_entry_comments_ref ON mobile_entry_comments(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_entry_reactions (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_entry_reactions_key ON mobile_entry_reactions(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_entry_reactions_owner ON mobile_entry_reactions(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_entry_reactions_ref ON mobile_entry_reactions(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_cases (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_cases_key ON mobile_cases(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_cases_owner ON mobile_cases(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_cases_ref ON mobile_cases(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_cases_media ON mobile_cases USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_claims (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_claims_key ON mobile_claims(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_claims_owner ON mobile_claims(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_claims_ref ON mobile_claims(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_claims_participants ON mobile_claims USING gin(participants);
CREATE INDEX IF NOT EXISTS idx_mobile_claims_media ON mobile_claims USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_leads (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_leads_key ON mobile_leads(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_leads_owner ON mobile_leads(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_leads_ref ON mobile_leads(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_leads_participants ON mobile_leads USING gin(participants);
CREATE INDEX IF NOT EXISTS idx_mobile_leads_media ON mobile_leads USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_subscriptions (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_subscriptions_key ON mobile_subscriptions(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_subscriptions_owner ON mobile_subscriptions(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_subscriptions_ref ON mobile_subscriptions(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_appointments (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_appointments_key ON mobile_appointments(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_appointments_owner ON mobile_appointments(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_appointments_ref ON mobile_appointments(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_appointments_participants ON mobile_appointments USING gin(participants);

CREATE TABLE IF NOT EXISTS mobile_adoptions (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_adoptions_key ON mobile_adoptions(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_adoptions_owner ON mobile_adoptions(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_adoptions_ref ON mobile_adoptions(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_adoptions_participants ON mobile_adoptions USING gin(participants);

CREATE TABLE IF NOT EXISTS mobile_care_updates (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_care_updates_key ON mobile_care_updates(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_care_updates_owner ON mobile_care_updates(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_care_updates_ref ON mobile_care_updates(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_care_updates_media ON mobile_care_updates USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_groups (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_groups_key ON mobile_groups(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_groups_owner ON mobile_groups(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_groups_ref ON mobile_groups(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_groups_participants ON mobile_groups USING gin(participants);

CREATE TABLE IF NOT EXISTS mobile_group_messages (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_group_messages_key ON mobile_group_messages(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_group_messages_owner ON mobile_group_messages(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_group_messages_ref ON mobile_group_messages(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_group_messages_media ON mobile_group_messages USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_invitations (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_invitations_key ON mobile_invitations(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_invitations_owner ON mobile_invitations(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_invitations_ref ON mobile_invitations(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_invitations_participants ON mobile_invitations USING gin(participants);

CREATE TABLE IF NOT EXISTS mobile_notifications (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_notifications_key ON mobile_notifications(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_notifications_owner ON mobile_notifications(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_notifications_ref ON mobile_notifications(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_preferences (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_preferences_key ON mobile_preferences(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_preferences_owner ON mobile_preferences(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_preferences_ref ON mobile_preferences(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_expressions (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_expressions_key ON mobile_expressions(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_expressions_owner ON mobile_expressions(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_expressions_ref ON mobile_expressions(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_drafts (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_drafts_key ON mobile_drafts(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_drafts_owner ON mobile_drafts(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_drafts_ref ON mobile_drafts(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_drafts_media ON mobile_drafts USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_media (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_media_key ON mobile_media(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_media_owner ON mobile_media(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_media_ref ON mobile_media(reference_id, id);

CREATE TABLE IF NOT EXISTS mobile_reviews (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_reviews_key ON mobile_reviews(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_reviews_owner ON mobile_reviews(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_reviews_ref ON mobile_reviews(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_reviews_media ON mobile_reviews USING gin(media_ids);

CREATE TABLE IF NOT EXISTS mobile_visit_feedback (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_visit_feedback_key ON mobile_visit_feedback(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_visit_feedback_owner ON mobile_visit_feedback(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_visit_feedback_ref ON mobile_visit_feedback(reference_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_visit_feedback_participants ON mobile_visit_feedback USING gin(participants);

CREATE TABLE IF NOT EXISTS mobile_outbox (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}',
    record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT,
    media_ids BIGINT[] NOT NULL DEFAULT '{}',
    public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_outbox_key ON mobile_outbox(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_outbox_owner ON mobile_outbox(owner_id, id);
CREATE INDEX IF NOT EXISTS idx_mobile_outbox_ref ON mobile_outbox(reference_id, id);

CREATE INDEX IF NOT EXISTS idx_mobile_entries_public ON mobile_entries(id) WHERE public AND state='published';
CREATE INDEX IF NOT EXISTS idx_mobile_cases_public ON mobile_cases(id) WHERE public AND state IN ('active','matched');
CREATE INDEX IF NOT EXISTS idx_mobile_outbox_pending ON mobile_outbox(id) WHERE state='pending';
ALTER TABLE mobile_claims ADD CONSTRAINT mobile_claim_case_fk FOREIGN KEY(reference_id) REFERENCES mobile_cases(id);
ALTER TABLE mobile_leads ADD CONSTRAINT mobile_lead_case_fk FOREIGN KEY(reference_id) REFERENCES mobile_cases(id);
ALTER TABLE mobile_entry_comments ADD CONSTRAINT mobile_comment_entry_fk FOREIGN KEY(reference_id) REFERENCES mobile_entries(id);
ALTER TABLE mobile_entry_reactions ADD CONSTRAINT mobile_reaction_entry_fk FOREIGN KEY(reference_id) REFERENCES mobile_entries(id);
ALTER TABLE mobile_group_messages ADD CONSTRAINT mobile_message_group_fk FOREIGN KEY(reference_id) REFERENCES mobile_groups(id);
ALTER TABLE mobile_care_updates ADD CONSTRAINT mobile_update_adoption_fk FOREIGN KEY(reference_id) REFERENCES mobile_adoptions(id);
ALTER TABLE mobile_visit_feedback ADD CONSTRAINT mobile_feedback_appointment_fk FOREIGN KEY(reference_id) REFERENCES mobile_appointments(id);
CREATE TABLE IF NOT EXISTS mobile_conversations (
 id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
 owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
 participants BIGINT[] NOT NULL DEFAULT '{}',
 record_key TEXT NOT NULL DEFAULT '',
 reference_id BIGINT,
 media_ids BIGINT[] NOT NULL DEFAULT '{}',
 public BOOLEAN NOT NULL DEFAULT FALSE,
 state TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS mobile_conversations_key ON mobile_conversations(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS mobile_conversations_owner ON mobile_conversations(owner_id, id);
CREATE INDEX IF NOT EXISTS mobile_conversations_reference ON mobile_conversations(reference_id, id);
CREATE INDEX IF NOT EXISTS mobile_conversations_participants ON mobile_conversations USING GIN(participants);

CREATE TABLE IF NOT EXISTS mobile_messages (
 id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
 owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
 participants BIGINT[] NOT NULL DEFAULT '{}',
 record_key TEXT NOT NULL DEFAULT '',
 reference_id BIGINT,
 media_ids BIGINT[] NOT NULL DEFAULT '{}',
 public BOOLEAN NOT NULL DEFAULT FALSE,
 state TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS mobile_messages_key ON mobile_messages(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS mobile_messages_owner ON mobile_messages(owner_id, id);
CREATE INDEX IF NOT EXISTS mobile_messages_reference ON mobile_messages(reference_id, id);
CREATE INDEX IF NOT EXISTS mobile_messages_media ON mobile_messages USING GIN(media_ids);

CREATE TABLE IF NOT EXISTS mobile_receipts (
 id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
 owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
 participants BIGINT[] NOT NULL DEFAULT '{}',
 record_key TEXT NOT NULL DEFAULT '',
 reference_id BIGINT,
 media_ids BIGINT[] NOT NULL DEFAULT '{}',
 public BOOLEAN NOT NULL DEFAULT FALSE,
 state TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS mobile_receipts_key ON mobile_receipts(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS mobile_receipts_owner ON mobile_receipts(owner_id, id);
CREATE INDEX IF NOT EXISTS mobile_receipts_reference ON mobile_receipts(reference_id, id);

ALTER TABLE mobile_messages ADD CONSTRAINT mobile_messages_conversation_fk FOREIGN KEY (reference_id) REFERENCES mobile_conversations(id);

CREATE TABLE IF NOT EXISTS mobile_offers (
 id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
 owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
 participants BIGINT[] NOT NULL DEFAULT '{}',
 record_key TEXT NOT NULL DEFAULT '',
 reference_id BIGINT REFERENCES mobile_pets(id),
 media_ids BIGINT[] NOT NULL DEFAULT '{}',
 public BOOLEAN NOT NULL DEFAULT FALSE,
 state TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS mobile_offers_key ON mobile_offers(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS mobile_offers_owner ON mobile_offers(owner_id,id);
CREATE INDEX IF NOT EXISTS mobile_offers_reference ON mobile_offers(reference_id,id);
CREATE INDEX IF NOT EXISTS mobile_offers_media ON mobile_offers USING GIN(media_ids);
CREATE INDEX IF NOT EXISTS mobile_offers_public ON mobile_offers(id) WHERE public=TRUE AND state='published';
COMMIT;
