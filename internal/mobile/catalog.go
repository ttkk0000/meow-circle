package mobile

import (
	_ "embed"
	"encoding/json"
)

//go:embed catalog.json
var expressionCatalogJSON []byte

func ExpressionCatalog() json.RawMessage { return append(json.RawMessage{}, expressionCatalogJSON...) }

func validFavorite(id string) bool {
	var catalogue struct {
		Emoji []struct {
			ID string `json:"id"`
		} `json:"emoji"`
		Stickers []struct {
			ID string `json:"id"`
		} `json:"stickers"`
	}
	if json.Unmarshal(expressionCatalogJSON, &catalogue) != nil {
		return false
	}
	for _, v := range catalogue.Emoji {
		if v.ID == id {
			return true
		}
	}
	for _, v := range catalogue.Stickers {
		if v.ID == id {
			return true
		}
	}
	return false
}
