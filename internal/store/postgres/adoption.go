package postgres

import (
	"context"
	"errors"
	"kitty-circle/internal/domain"

	"github.com/jackc/pgx/v5"
)

// Adoption methods
func (s *Store) CreateAdoptionPet(pet domain.AdoptionPet) domain.AdoptionPet {
	ctx, cancel := bg()
	defer cancel()

	const query = `
		INSERT INTO adoption_pets (rescuer_id, name, species, breed, age, city, health, description, status)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
		RETURNING id, created_at, updated_at
	`
	err := s.pool.QueryRow(ctx, query,
		pet.RescuerID, pet.Name, pet.Species, pet.Breed, pet.Age, pet.City, pet.Health, pet.Description, pet.Status,
	).Scan(&pet.ID, &pet.CreatedAt, &pet.UpdatedAt)
	if err != nil {
		logErr("CreateAdoptionPet", err)
	}

	if len(pet.MediaIDs) > 0 {
		b := &pgx.Batch{}
		for i, mid := range pet.MediaIDs {
			b.Queue(`INSERT INTO adoption_pet_media (pet_id, media_id, position) VALUES ($1, $2, $3)`, pet.ID, mid, i)
		}
		if br := s.pool.SendBatch(ctx, b); br != nil {
			br.Close()
		}
	}

	return pet
}

func (s *Store) GetAdoptionPet(id int64) (domain.AdoptionPet, bool) {
	ctx, cancel := bg()
	defer cancel()

	var p domain.AdoptionPet
	const query = `
		SELECT id, rescuer_id, name, species, breed, age, city, health, description, status, created_at, updated_at
		FROM adoption_pets WHERE id = $1
	`
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&p.ID, &p.RescuerID, &p.Name, &p.Species, &p.Breed, &p.Age,
		&p.City, &p.Health, &p.Description, &p.Status, &p.CreatedAt, &p.UpdatedAt,
	)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetAdoptionPet", err)
		}
		return p, false
	}

	p.MediaIDs = s.getAdoptionPetMedia(ctx, p.ID)
	return p, true
}

func (s *Store) getAdoptionPetMedia(ctx context.Context, petID int64) []int64 {
	rows, err := s.pool.Query(ctx, `SELECT media_id FROM adoption_pet_media WHERE pet_id = $1 ORDER BY position`, petID)
	if err != nil {
		logErr("getAdoptionPetMedia", err)
		return nil
	}
	defer rows.Close()
	var ids []int64
	for rows.Next() {
		var id int64
		if err := rows.Scan(&id); err == nil {
			ids = append(ids, id)
		}
	}
	return ids
}

func (s *Store) ListAdoptionPets() []domain.AdoptionPet {
	ctx, cancel := bg()
	defer cancel()

	rows, err := s.pool.Query(ctx, `
		SELECT id, rescuer_id, name, species, breed, age, city, health, description, status, created_at, updated_at
		FROM adoption_pets ORDER BY created_at DESC
	`)
	if err != nil {
		logErr("ListAdoptionPets", err)
		return nil
	}
	defer rows.Close()

	var pets []domain.AdoptionPet
	for rows.Next() {
		var p domain.AdoptionPet
		if err := rows.Scan(
			&p.ID, &p.RescuerID, &p.Name, &p.Species, &p.Breed, &p.Age,
			&p.City, &p.Health, &p.Description, &p.Status, &p.CreatedAt, &p.UpdatedAt,
		); err == nil {
			p.MediaIDs = s.getAdoptionPetMedia(ctx, p.ID)
			pets = append(pets, p)
		}
	}
	return pets
}

func (s *Store) ListAdoptionPetsByRescuer(rescuerID int64) []domain.AdoptionPet {
	ctx, cancel := bg()
	defer cancel()

	rows, err := s.pool.Query(ctx, `
		SELECT id, rescuer_id, name, species, breed, age, city, health, description, status, created_at, updated_at
		FROM adoption_pets WHERE rescuer_id = $1 ORDER BY created_at DESC
	`, rescuerID)
	if err != nil {
		logErr("ListAdoptionPetsByRescuer", err)
		return nil
	}
	defer rows.Close()

	var pets []domain.AdoptionPet
	for rows.Next() {
		var p domain.AdoptionPet
		if err := rows.Scan(
			&p.ID, &p.RescuerID, &p.Name, &p.Species, &p.Breed, &p.Age,
			&p.City, &p.Health, &p.Description, &p.Status, &p.CreatedAt, &p.UpdatedAt,
		); err == nil {
			p.MediaIDs = s.getAdoptionPetMedia(ctx, p.ID)
			pets = append(pets, p)
		}
	}
	return pets
}

func (s *Store) CreateAdoptionApplication(app domain.AdoptionApplication) domain.AdoptionApplication {
	ctx, cancel := bg()
	defer cancel()

	const query = `
		INSERT INTO adoption_applications (pet_id, applicant_id, rescuer_id, status, message, contact_info)
		VALUES ($1, $2, $3, $4, $5, $6)
		RETURNING id, created_at, updated_at
	`
	err := s.pool.QueryRow(ctx, query,
		app.PetID, app.ApplicantID, app.RescuerID, app.Status, app.Message, app.ContactInfo,
	).Scan(&app.ID, &app.CreatedAt, &app.UpdatedAt)
	if err != nil {
		logErr("CreateAdoptionApplication", err)
	}
	return app
}

func (s *Store) GetAdoptionApplication(id int64) (domain.AdoptionApplication, bool) {
	ctx, cancel := bg()
	defer cancel()

	var a domain.AdoptionApplication
	const query = `
		SELECT id, pet_id, applicant_id, rescuer_id, status, message, contact_info, created_at, updated_at
		FROM adoption_applications WHERE id = $1
	`
	err := s.pool.QueryRow(ctx, query, id).Scan(
		&a.ID, &a.PetID, &a.ApplicantID, &a.RescuerID, &a.Status, &a.Message, &a.ContactInfo, &a.CreatedAt, &a.UpdatedAt,
	)
	if err != nil {
		if !errors.Is(err, pgx.ErrNoRows) {
			logErr("GetAdoptionApplication", err)
		}
		return a, false
	}
	return a, true
}

func (s *Store) ListAdoptionApplicationsByApplicant(applicantID int64) []domain.AdoptionApplication {
	ctx, cancel := bg()
	defer cancel()

	rows, err := s.pool.Query(ctx, `
		SELECT id, pet_id, applicant_id, rescuer_id, status, message, contact_info, created_at, updated_at
		FROM adoption_applications WHERE applicant_id = $1 ORDER BY created_at DESC
	`, applicantID)
	if err != nil {
		logErr("ListAdoptionApplicationsByApplicant", err)
		return nil
	}
	defer rows.Close()

	var apps []domain.AdoptionApplication
	for rows.Next() {
		var a domain.AdoptionApplication
		if err := rows.Scan(
			&a.ID, &a.PetID, &a.ApplicantID, &a.RescuerID, &a.Status, &a.Message, &a.ContactInfo, &a.CreatedAt, &a.UpdatedAt,
		); err == nil {
			apps = append(apps, a)
		}
	}
	return apps
}

func (s *Store) ListAdoptionApplicationsByRescuer(rescuerID int64) []domain.AdoptionApplication {
	ctx, cancel := bg()
	defer cancel()

	rows, err := s.pool.Query(ctx, `
		SELECT id, pet_id, applicant_id, rescuer_id, status, message, contact_info, created_at, updated_at
		FROM adoption_applications WHERE rescuer_id = $1 ORDER BY created_at DESC
	`, rescuerID)
	if err != nil {
		logErr("ListAdoptionApplicationsByRescuer", err)
		return nil
	}
	defer rows.Close()

	var apps []domain.AdoptionApplication
	for rows.Next() {
		var a domain.AdoptionApplication
		if err := rows.Scan(
			&a.ID, &a.PetID, &a.ApplicantID, &a.RescuerID, &a.Status, &a.Message, &a.ContactInfo, &a.CreatedAt, &a.UpdatedAt,
		); err == nil {
			apps = append(apps, a)
		}
	}
	return apps
}

func (s *Store) UpdateAdoptionApplicationStatus(id int64, status domain.ApplicationStatus) bool {
	ctx, cancel := bg()
	defer cancel()

	cmd, err := s.pool.Exec(ctx, `UPDATE adoption_applications SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`, status, id)
	if err != nil {
		logErr("UpdateAdoptionApplicationStatus", err)
		return false
	}
	return cmd.RowsAffected() > 0
}
