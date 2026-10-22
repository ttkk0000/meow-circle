ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS category TEXT NOT NULL DEFAULT 'product';

CREATE INDEX IF NOT EXISTS idx_listings_category ON listings(category);
