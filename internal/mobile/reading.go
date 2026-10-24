package mobile

// 阅读材料是公开只读发布物；草案不充当已经生效的协议或实际经营资料。
type ReadingFact struct {
	Label string `json:"label"`
	Text  string `json:"text"`
}

type ReadingDocument struct {
	Key            string        `json:"key"`
	Type           string        `json:"type"`
	Title          string        `json:"title"`
	Heading        string        `json:"heading"`
	Revision       string        `json:"revision"`
	Status         string        `json:"status"`
	EffectiveFrom  string        `json:"effective_from"`
	EffectiveUntil string        `json:"effective_until"`
	Notice         string        `json:"notice"`
	Facts          []ReadingFact `json:"facts"`
	Paragraphs     []string      `json:"paragraphs"`
	Changes        []string      `json:"changes"`
	CoverAsset     string        `json:"cover_asset"`
	SHA256         string        `json:"sha256"`
}

type ReadingCatalog struct {
	OperatorName    string            `json:"operator_name"`
	OperatorAddress string            `json:"operator_address"`
	OperatorContact string            `json:"operator_contact"`
	Documents       []ReadingDocument `json:"documents"`
}
