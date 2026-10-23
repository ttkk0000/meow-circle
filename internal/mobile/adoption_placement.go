package mobile

import "fmt"

// The pet row is the common lock for all of its application records. A
// per-application version alone cannot prevent two different trial placements.
func adoptionOccupies(state string) bool {
	return HasString([]string{"trial_active", "handover_pending", "return_requested", "return_agreed"}, state)
}

func adoptionTerminal(state string) bool {
	return HasString([]string{"rejected", "withdrawn", "completed", "returned", "closed_other_placement", "deleted"}, state)
}

func adoptionKey(pet, applicant, cycle int64) string {
	if cycle == 0 {
		return fmt.Sprintf("%d:%d", pet, applicant)
	}
	return fmt.Sprintf("%d:%d:%d", pet, applicant, cycle)
}

func resetAdoptionConsent(a *Adoption) {
	for i := range a.Contracts {
		switch a.Contracts[i].Status {
		case "pending":
			a.Contracts[i].Status = "superseded"
		case "effective":
			a.Contracts[i].Status = "replaced"
		}
	}
	a.EffectiveContract = 0
	a.TrialConfirmations = nil
	a.HandoverConfirmations = nil
	a.TrialUntil = nil
	a.TrialTerms = ""
}

func adoptionPet(tx Tx, a Adoption) (Record, Pet, error) {
	r, e := tx.Get(Pets, a.PetID)
	if e != nil {
		return r, Pet{}, e
	}
	p, e := Decode[Pet](r)
	if e == nil && p.AdoptionCycle != a.AdoptionCycle {
		e = ErrConflict
	}
	return r, p, e
}

func otherPlacement(tx Tx, petID, applicationID int64) (bool, error) {
	rows, e := allRecords(tx, Adoptions, Filter{RefID: petID})
	if e != nil {
		return false, e
	}
	for _, row := range rows {
		if row.ID != applicationID && adoptionOccupies(row.State) {
			return true, nil
		}
	}
	return false, nil
}

func (s *Service) reserveAdoption(tx Tx, actor int64, r Record, a Adoption, state string) error {
	for _, party := range []int64{a.PublisherID, a.ApplicantID} {
		if e := s.adult(tx, party); e != nil {
			return e
		}
	}
	pet, p, e := adoptionPet(tx, a)
	if e != nil {
		return e
	}
	if pet.OwnerID != a.PublisherID || pet.State != "active" || !p.AdoptionAvailable && !adoptionOccupies(r.State) {
		return ErrConflict
	}
	occupied, e := otherPlacement(tx, pet.ID, r.ID)
	if e != nil {
		return e
	}
	if occupied {
		return Denied("pet_placement_conflict")
	}
	rows, e := allRecords(tx, Adoptions, Filter{RefID: pet.ID})
	if e != nil {
		return e
	}
	for _, row := range rows {
		if row.ID == r.ID || !HasString([]string{"approved", "trial_proposed"}, row.State) {
			continue
		}
		other, e := Decode[Adoption](row)
		if e != nil {
			return e
		}
		row.State = "waitlisted"
		other.TrialConfirmations = nil
		other.TrialUntil = nil
		if e = save(tx, Adoptions, &row, other, actor, "adoption.waitlisted"); e != nil {
			return e
		}
		if e = s.notify(tx, other.ApplicantID, actor, "applications", Adoptions, row.ID, "当前已有领养安排，你的申请进入候补"); e != nil {
			return e
		}
	}
	p.PlacementState = state
	return save(tx, Pets, &pet, p, actor, "pet.placement_reserved")
}

func (s *Service) closeOtherAdoptions(tx Tx, actor int64, current Record, notice string) error {
	rows, e := allRecords(tx, Adoptions, Filter{RefID: current.RefID})
	if e != nil {
		return e
	}
	for _, row := range rows {
		if row.ID == current.ID || adoptionTerminal(row.State) {
			continue
		}
		a, e := Decode[Adoption](row)
		if e != nil {
			return e
		}
		row.State = "closed_other_placement"
		if e = save(tx, Adoptions, &row, a, actor, "adoption.closed_other_placement"); e != nil {
			return e
		}
		if e = s.notify(tx, a.ApplicantID, actor, "applications", Adoptions, row.ID, notice); e != nil {
			return e
		}
	}
	return nil
}
