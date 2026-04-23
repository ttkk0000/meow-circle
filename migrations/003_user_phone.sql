-- Optional normalized phone (digits only) for login + registration verification.
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone TEXT NOT NULL DEFAULT '';

-- At most one account per non-empty phone.
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_unique
    ON users (phone)
    WHERE phone <> '';
