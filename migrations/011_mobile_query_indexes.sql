-- 只增加索引；不改写业务记录，不替代证据保留或权限筛选。
BEGIN;
DO $$
DECLARE
    module TEXT;
    table_name TEXT;
    column_name TEXT;
    column_number SMALLINT;
BEGIN
    FOREACH module IN ARRAY ARRAY['accounts', 'sessions', 'identities', 'auth_challenges', 'otp_challenges', 'friendships', 'pets', 'offers', 'entries', 'entry_comments', 'pet_comments', 'comment_reactions', 'entry_reactions', 'cases', 'claims', 'leads', 'subscriptions', 'appointments', 'adoptions', 'care_updates', 'groups', 'group_messages', 'conversations', 'messages', 'receipts', 'invitations', 'notifications', 'preferences', 'expressions', 'drafts', 'media', 'reviews', 'visit_feedback', 'outbox', 'qualifications', 'guardians', 'operations', 'deliveries', 'data_requests', 'rights_cases', 'authorities', 'audit_entries', 'push_devices', 'revenue_ledger'] LOOP
        table_name := 'mobile_' || module;
        FOREACH column_name IN ARRAY ARRAY['media_ids', 'participants'] LOOP
            SELECT attnum INTO STRICT column_number FROM pg_attribute
              WHERE attrelid = table_name::regclass AND attname = column_name AND NOT attisdropped;
            -- 007已给部分表建了同等GIN索引，不再重复建立。
            IF NOT EXISTS (
                SELECT 1 FROM pg_index i
                JOIN pg_class c ON c.oid = i.indexrelid
                JOIN pg_am a ON a.oid = c.relam
                WHERE i.indrelid = table_name::regclass AND i.indisvalid
                  AND i.indpred IS NULL AND i.indexprs IS NULL
                  AND i.indnatts = 1 AND i.indkey[0] = column_number AND a.amname = 'gin'
            ) THEN
                EXECUTE format('CREATE INDEX %I ON %I USING GIN (%I)', 'idx_' || table_name || '_' || column_name, table_name, column_name);
            END IF;
        END LOOP;
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (record_key text_pattern_ops) WHERE record_key <> ''''', 'idx_' || table_name || '_key_prefix', table_name);
    END LOOP;
END $$;
CREATE INDEX IF NOT EXISTS idx_mobile_adoptions_ref_state ON mobile_adoptions(reference_id, state, id);
CREATE INDEX IF NOT EXISTS idx_mobile_guardians_ref_state ON mobile_guardians(reference_id, state, id);
COMMIT;
