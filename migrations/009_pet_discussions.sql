-- Apply after 008. No examples or operational identities are seeded.
BEGIN;
CREATE TABLE IF NOT EXISTS mobile_pet_comments (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}', record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT, media_ids BIGINT[] NOT NULL DEFAULT '{}', public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL, version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_pet_comments_key ON mobile_pet_comments(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_pet_comments_owner ON mobile_pet_comments(owner_id,id);
CREATE INDEX IF NOT EXISTS idx_mobile_pet_comments_ref ON mobile_pet_comments(reference_id,id);
CREATE TABLE IF NOT EXISTS mobile_comment_reactions (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}', record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT, media_ids BIGINT[] NOT NULL DEFAULT '{}', public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL, version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_comment_reactions_key ON mobile_comment_reactions(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_comment_reactions_owner ON mobile_comment_reactions(owner_id,id);
CREATE INDEX IF NOT EXISTS idx_mobile_comment_reactions_ref ON mobile_comment_reactions(reference_id,id);
COMMIT;
