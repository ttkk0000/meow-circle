package mobile

func (s *Service) appointmentAdmission(tx Tx, kind string, id int64, terms AppointmentTerms) error {
	species, city := "", ""
	switch kind {
	case "case":
		r, e := tx.Get(Cases, id)
		if e != nil {
			return e
		}
		v, e := Decode[Case](r)
		if e != nil {
			return e
		}
		species, city = v.Species, v.City
	case "offer":
		r, e := tx.Get(Offers, id)
		if e != nil {
			return e
		}
		v, e := Decode[Offer](r)
		if e != nil {
			return e
		}
		city = v.City
		pet, e := tx.Get(Pets, v.PetID)
		if e != nil {
			return e
		}
		p, e := Decode[Pet](pet)
		if e != nil {
			return e
		}
		species = p.Species
	case "visit":
		r, e := tx.Get(Pets, id)
		if e != nil {
			return e
		}
		v, e := Decode[Pet](r)
		if e != nil {
			return e
		}
		species = v.Species
	case "adoption":
		r, e := tx.Get(Adoptions, id)
		if e != nil {
			return e
		}
		v, e := Decode[Adoption](r)
		if e != nil {
			return e
		}
		pet, e := tx.Get(Pets, v.PetID)
		if e != nil {
			return e
		}
		p, e := Decode[Pet](pet)
		if e != nil {
			return e
		}
		species = p.Species
	default:
		return Invalid("invalid appointment context")
	}
	if species != terms.Species || city != "" && city != terms.City {
		return Denied("appointment_context_mismatch")
	}
	return s.cityAdmission(tx, terms.City, terms.Species, "offline")
}
