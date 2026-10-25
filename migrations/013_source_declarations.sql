-- 声明独立于发布；更正追加新记录，既有记录不可通过消费者API修改。
BEGIN;
CREATE TABLE IF NOT EXISTS mobile_source_declarations (
    id BIGINT PRIMARY KEY DEFAULT nextval('mobile_id_seq'),
    owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    participants BIGINT[] NOT NULL DEFAULT '{}', record_key TEXT NOT NULL DEFAULT '',
    reference_id BIGINT, media_ids BIGINT[] NOT NULL DEFAULT '{}', public BOOLEAN NOT NULL DEFAULT FALSE,
    state TEXT NOT NULL, version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_mobile_source_declarations_key ON mobile_source_declarations(record_key) WHERE record_key <> '';
CREATE INDEX IF NOT EXISTS idx_mobile_source_declarations_owner ON mobile_source_declarations(owner_id,id);
CREATE INDEX IF NOT EXISTS idx_mobile_source_declarations_ref ON mobile_source_declarations(reference_id,id);
CREATE INDEX IF NOT EXISTS idx_mobile_source_declarations_owner_state ON mobile_source_declarations(owner_id,state,id);
COMMIT;
