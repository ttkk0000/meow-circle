package mobileapi

import "kitty-circle/internal/mobile"

// 只返回当前运营身份及已发布材料，绝不返回配置中的Token或其他人员凭据。
type OperatorContext struct {
	ID              string                   `json:"id"`
	Roles           []string                 `json:"roles"`
	OperatorName    string                   `json:"operator_name"`
	OperatorAddress string                   `json:"operator_address"`
	OperatorContact string                   `json:"operator_contact"`
	PublishedTypes  []string                 `json:"published_types"`
	Documents       []mobile.ReadingDocument `json:"documents"`
}

func operatorContext(op Operator) (OperatorContext, error) {
	catalog, err := loadReadingCatalog()
	if err != nil {
		return OperatorContext{}, err
	}
	out := OperatorContext{ID: op.ID, Roles: op.Roles, OperatorName: catalog.OperatorName,
		OperatorAddress: catalog.OperatorAddress, OperatorContact: catalog.OperatorContact,
		PublishedTypes: []string{}, Documents: catalog.Documents}
	for _, document := range catalog.Documents {
		if document.Status == "published" {
			out.PublishedTypes = append(out.PublishedTypes, document.Type)
		}
	}
	return out, nil
}
