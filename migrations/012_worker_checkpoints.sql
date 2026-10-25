BEGIN;
CREATE TABLE IF NOT EXISTS mnd_worker_checkpoints (
    job TEXT PRIMARY KEY CHECK (job IN ('push','nearby_alerts','content_updates','privacy_cleanup','provider_revocations','maintenance','search_projection','audit_projection')),
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    positions JSONB NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(positions) = 'object'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
COMMIT;
