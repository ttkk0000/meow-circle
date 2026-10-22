package api

import (
	"net/http"
	"strconv"
	"strings"

	"kitty-circle/internal/domain"
)

const adoptionPetStatusAvailable = "available"

func (r *Router) handleAdoptionPets(w http.ResponseWriter, req *http.Request) {
	if req.Method == http.MethodGet {
		pets := filterAdoptionPets(req, r.store.ListAdoptionPets())
		writeOK(w, map[string]any{"pets": pets})
		return
	}
	writeError(w, http.StatusMethodNotAllowed, "method not allowed")
}

func (r *Router) handleAdoptionPetChild(w http.ResponseWriter, req *http.Request) {
	parts := strings.Split(strings.TrimPrefix(req.URL.Path, "/api/v1/adoption/pets/"), "/")
	if len(parts) == 0 || parts[0] == "" {
		writeError(w, http.StatusBadRequest, "missing pet id")
		return
	}

	id, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid pet id")
		return
	}

	pet, ok := r.store.GetAdoptionPet(id)
	if !ok {
		writeError(w, http.StatusNotFound, "pet not found")
		return
	}

	if req.Method == http.MethodGet {
		rescuer, _ := r.store.GetUser(pet.RescuerID)
		writeOK(w, map[string]any{
			"pet":     pet,
			"rescuer": rescuer,
		})
		return
	}
	writeError(w, http.StatusMethodNotAllowed, "method not allowed")
}

func (r *Router) handleAdoptionApplications(w http.ResponseWriter, req *http.Request) {
	user, ok := currentUser(req)
	if !ok {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	if req.Method == http.MethodPost {
		var input struct {
			PetID       int64  `json:"pet_id"`
			Message     string `json:"message"`
			ContactInfo string `json:"contact_info"`
		}
		if err := decodeJSON(req, &input); err != nil {
			writeError(w, http.StatusBadRequest, err.Error())
			return
		}

		message := strings.TrimSpace(input.Message)
		contactInfo := strings.TrimSpace(input.ContactInfo)
		if input.PetID <= 0 {
			writeError(w, http.StatusBadRequest, "pet_id is required")
			return
		}
		if message == "" {
			writeError(w, http.StatusBadRequest, "message is required")
			return
		}
		if contactInfo == "" {
			writeError(w, http.StatusBadRequest, "contact_info is required")
			return
		}

		pet, ok := r.store.GetAdoptionPet(input.PetID)
		if !ok {
			writeError(w, http.StatusNotFound, "pet not found")
			return
		}
		if pet.RescuerID == user.ID {
			writeError(w, http.StatusBadRequest, "cannot apply to your own adoption pet")
			return
		}
		if strings.TrimSpace(pet.Status) != "" && !strings.EqualFold(pet.Status, adoptionPetStatusAvailable) {
			writeError(w, http.StatusConflict, "pet is not available")
			return
		}
		for _, existing := range r.store.ListAdoptionApplicationsByApplicant(user.ID) {
			if existing.PetID == pet.ID && applicationStillActive(existing.Status) {
				writeError(w, http.StatusConflict, "application already exists")
				return
			}
		}

		app := r.store.CreateAdoptionApplication(domain.AdoptionApplication{
			PetID:       pet.ID,
			ApplicantID: user.ID,
			RescuerID:   pet.RescuerID,
			Status:      domain.ApplicationStatusSubmitted,
			Message:     message,
			ContactInfo: contactInfo,
		})
		r.notify(
			pet.RescuerID,
			domain.NotificationSystem,
			"New adoption application",
			user.Nickname+" wants to learn about "+pet.Name,
			app.ID,
			notifyActor(user),
			notifyImageURL(r.firstMediaURL(pet.MediaIDs)),
		)

		writeOK(w, map[string]any{"application": app})
		return
	}
	writeError(w, http.StatusMethodNotAllowed, "method not allowed")
}

func (r *Router) handleMyAdoptionApplications(w http.ResponseWriter, req *http.Request) {
	if req.Method != http.MethodGet {
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
		return
	}

	user, ok := currentUser(req)
	if !ok {
		writeError(w, http.StatusUnauthorized, "unauthorized")
		return
	}
	apps := r.store.ListAdoptionApplicationsByApplicant(user.ID)

	writeOK(w, map[string]any{"applications": apps})
}

func filterAdoptionPets(req *http.Request, pets []domain.AdoptionPet) []domain.AdoptionPet {
	q := req.URL.Query()
	species := strings.ToLower(strings.TrimSpace(q.Get("species")))
	city := strings.ToLower(strings.TrimSpace(q.Get("city")))
	status := strings.ToLower(strings.TrimSpace(q.Get("status")))
	if q.Get("available") == "true" && status == "" {
		status = adoptionPetStatusAvailable
	}
	if species == "" && city == "" && status == "" {
		return pets
	}

	filtered := make([]domain.AdoptionPet, 0, len(pets))
	for _, pet := range pets {
		if species != "" && !matchesSpecies(pet.Species, species) {
			continue
		}
		if city != "" && !strings.Contains(strings.ToLower(pet.City), city) {
			continue
		}
		if status != "" && !strings.EqualFold(pet.Status, status) {
			continue
		}
		filtered = append(filtered, pet)
	}
	return filtered
}

func matchesSpecies(value, expected string) bool {
	normalized := strings.ToLower(strings.TrimSpace(value))
	switch expected {
	case "cat", "cats":
		return strings.Contains(normalized, "cat")
	case "dog", "dogs", "doggie":
		return strings.Contains(normalized, "dog")
	default:
		return strings.Contains(normalized, expected)
	}
}

func applicationStillActive(status domain.ApplicationStatus) bool {
	switch status {
	case domain.ApplicationStatusRejected, domain.ApplicationStatusWithdrawn:
		return false
	default:
		return true
	}
}
