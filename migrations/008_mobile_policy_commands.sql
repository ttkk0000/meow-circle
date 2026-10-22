-- Apply after 007. Unknown age/guardian/adult eligibility is intentionally not seeded.
-- Existing case-folded username conflicts must be resolved explicitly before deployment.
BEGIN;
DO $$
DECLARE module TEXT;
BEGIN
    IF EXISTS (SELECT lower(username) FROM users GROUP BY lower(username) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'case-folded username conflict: resolve account ownership before migration';
    END IF;
    FOREACH module IN ARRAY ARRAY['qualifications','guardians','operations','deliveries','data_requests','rights_cases','authorities','audit_entries','push_devices','revenue_ledger'] LOOP
        EXECUTE format('CREATE TABLE %I (
            id BIGINT PRIMARY KEY DEFAULT nextval(''mobile_id_seq''),
            owner_id BIGINT REFERENCES users(id) ON DELETE RESTRICT,
            participants BIGINT[] NOT NULL DEFAULT ''{}'', record_key TEXT NOT NULL DEFAULT '''',
            reference_id BIGINT, media_ids BIGINT[] NOT NULL DEFAULT ''{}'', public BOOLEAN NOT NULL DEFAULT FALSE,
            state TEXT NOT NULL, version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            data JSONB NOT NULL CHECK (jsonb_typeof(data) = ''object''))', 'mobile_' || module);
        EXECUTE format('CREATE UNIQUE INDEX ON %I(record_key) WHERE record_key <> ''''', 'mobile_' || module);
        EXECUTE format('CREATE INDEX ON %I(owner_id, id)', 'mobile_' || module);
        EXECUTE format('CREATE INDEX ON %I(reference_id, id)', 'mobile_' || module);
        EXECUTE format('CREATE INDEX ON %I(state, id)', 'mobile_' || module);
    END LOOP;
END $$;
CREATE UNIQUE INDEX uq_users_username_folded ON users(lower(username));
COMMIT;
