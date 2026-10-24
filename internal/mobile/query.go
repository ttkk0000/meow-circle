package mobile

import "strings"

type Query struct {
	Space, State, Keyword, City, Species string
	OwnerID                              int64
	Following, Watched, Adoption         bool
	Latitude, Longitude, RadiusKM        float64
}

func matches(k Kind, r Record, q Query) (bool, error) {
	if q.State != "" && r.State != q.State || q.OwnerID > 0 && r.OwnerID != q.OwnerID {
		return false, nil
	}
	text, city, species := "", "", ""
	switch k {
	case Entries:
		v, e := Decode[Entry](r)
		if e != nil {
			return false, e
		}
		if q.Space != "" && q.Space != v.Space {
			return false, nil
		}
		text = v.Title + " " + v.Text
		city = v.City
	case Pets:
		v, e := Decode[Pet](r)
		if e != nil {
			return false, e
		}
		if q.Adoption && !v.AdoptionAvailable {
			return false, nil
		}
		text = v.Name + " " + v.Breed + " " + v.Bio
		city = v.City
		species = v.Species
	case Cases:
		v, e := Decode[Case](r)
		if e != nil {
			return false, e
		}
		text = v.Title + " " + v.Description
		city = v.City
		species = v.Species
		if q.RadiusKM > 0 && (v.LocationPrecision == "area" || distanceKM(v.Latitude, v.Longitude, q.Latitude, q.Longitude) > q.RadiusKM) {
			return false, nil
		}
	case Offers:
		v, e := Decode[Offer](r)
		if e != nil {
			return false, e
		}
		text = v.Custodian + " " + v.Origin
		city = v.City
	}
	if q.City != "" && city != q.City || q.Species != "" && species != q.Species {
		return false, nil
	}
	return q.Keyword == "" || strings.Contains(strings.ToLower(text), strings.ToLower(q.Keyword)), nil
}
