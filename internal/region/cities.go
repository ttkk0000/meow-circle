// Package region supplies the bundled mainland city catalog. Administrative
// centres describe map viewports, never a user's or a missing animal's location.
package region

import (
	_ "embed"
	"encoding/json"
	"strings"
)

//go:embed mainland-cities.json
var cityData []byte

type City struct {
	Code  string `json:"code"`
	Name  string `json:"name"`
	Value string `json:"value"`
	Alias string `json:"alias"`
}

var cityIndex = func() map[string]City {
	var catalog struct {
		Cities []City `json:"cities"`
	}
	if err := json.Unmarshal(cityData, &catalog); err != nil {
		panic(err)
	}
	index := make(map[string]City)
	aliases := make(map[string][]City)
	for _, city := range catalog.Cities {
		index[city.Code], index[city.Name], index[city.Value] = city, city, city
		aliases[city.Alias] = append(aliases[city.Alias], city)
	}
	for alias, cities := range aliases {
		if len(cities) == 1 {
			if _, exists := index[alias]; !exists {
				index[alias] = cities[0]
			}
		}
	}
	return index
}()

func Lookup(value string) (City, bool) {
	city, ok := cityIndex[strings.TrimSpace(value)]
	return city, ok
}

func Normalize(value string) string {
	if city, ok := Lookup(value); ok {
		return city.Value
	}
	return strings.TrimSpace(value)
}

func Same(a, b string) bool { return Normalize(a) == Normalize(b) }
