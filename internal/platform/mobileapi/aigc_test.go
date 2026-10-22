package mobileapi

import (
	"bytes"
	"image"
	"image/png"
	"kitty-circle/internal/mobile"
	"testing"
)

func TestAIGCLabelRoundTripAfterPrivacyEncoding(t *testing.T) {
	var clean bytes.Buffer
	if e := png.Encode(&clean, image.NewRGBA(image.Rect(0, 0, 2, 2))); e != nil {
		t.Fatal(e)
	}
	label, e := mobile.PropagationLabel("", "registered-provider-code", "content-reference", "1")
	if e != nil {
		t.Fatal(e)
	}
	marked, e := embedAIGC(clean.Bytes(), "image/png", label)
	if e != nil {
		t.Fatal(e)
	}
	got, known, e := extractAIGC(marked, "png")
	if e != nil || !known || got != label {
		t.Fatal(got, known, e)
	}
	if _, e = png.Decode(bytes.NewReader(marked)); e != nil {
		t.Fatal("invalid PNG CRC or chunk structure", e)
	}
}
func TestUnconfiguredPropagationAndMalformedMarkerFailClosed(t *testing.T) {
	if _, e := mobile.PropagationLabel("", "", "id", "1"); e == nil {
		t.Fatal("invented propagator accepted")
	}
	if _, _, e := extractAIGC([]byte{137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 255, 't', 'E', 'X', 't'}, "png"); e == nil {
		t.Fatal("invalid chunk accepted")
	}
}
