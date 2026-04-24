-- Rich notification payload for UI (actor + optional thumbnail).
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS actor_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS actor_username TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS actor_nickname TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS actor_avatar_url TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS image_url TEXT NOT NULL DEFAULT '';
