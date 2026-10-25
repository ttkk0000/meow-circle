-- 独立技术运维记录；不创建账号、凭据、示例事件或发布记录。
BEGIN;
CREATE TABLE IF NOT EXISTS mnd_ops_records (
    environment TEXT NOT NULL CHECK (environment ~ '^[a-z][a-z0-9_-]{0,39}$'),
    kind TEXT NOT NULL CHECK (kind IN ('runs','incidents','releases','audit','commands')),
    record_id TEXT NOT NULL,
    sequence BIGSERIAL UNIQUE NOT NULL,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    state TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    data JSONB NOT NULL CHECK (jsonb_typeof(data) = 'object'),
    PRIMARY KEY(environment,kind,record_id)
);
CREATE INDEX IF NOT EXISTS idx_mnd_ops_records_page ON mnd_ops_records(environment,kind,sequence DESC);
CREATE INDEX IF NOT EXISTS idx_mnd_ops_records_state ON mnd_ops_records(environment,kind,state,sequence DESC);
CREATE INDEX IF NOT EXISTS idx_mnd_ops_records_subject ON mnd_ops_records(environment,kind,(COALESCE(data->>'job',data->>'service')),sequence DESC);
COMMIT;
