CREATE TABLE IF NOT EXISTS adoption_pets (
    id          BIGSERIAL PRIMARY KEY,
    rescuer_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    species     TEXT NOT NULL,
    breed       TEXT NOT NULL,
    age         TEXT NOT NULL,
    city        TEXT NOT NULL,
    health      TEXT NOT NULL,
    description TEXT NOT NULL,
    status      TEXT NOT NULL DEFAULT 'available',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS adoption_pet_media (
    pet_id   BIGINT NOT NULL REFERENCES adoption_pets(id) ON DELETE CASCADE,
    media_id BIGINT NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    position INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (pet_id, media_id)
);

CREATE TABLE IF NOT EXISTS adoption_applications (
    id           BIGSERIAL PRIMARY KEY,
    pet_id       BIGINT NOT NULL REFERENCES adoption_pets(id) ON DELETE CASCADE,
    applicant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rescuer_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status       TEXT NOT NULL DEFAULT 'submitted',
    message      TEXT NOT NULL,
    contact_info TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_adoption_pets_rescuer ON adoption_pets(rescuer_id);
CREATE INDEX IF NOT EXISTS idx_adoption_pets_status ON adoption_pets(status);
CREATE INDEX IF NOT EXISTS idx_adoption_pets_species_city ON adoption_pets(species, city);
CREATE INDEX IF NOT EXISTS idx_adoption_apps_applicant ON adoption_applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_adoption_apps_rescuer ON adoption_applications(rescuer_id);
CREATE INDEX IF NOT EXISTS idx_adoption_apps_pet ON adoption_applications(pet_id);
