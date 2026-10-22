package mobileapi

import (
	"bytes"
	"compress/zlib"
	"encoding/binary"
	"encoding/json"
	"hash/crc32"
	"html"
	"io"
	"regexp"
	"strings"

	"kitty-circle/internal/mobile"
)

func extractAIGC(raw []byte, format string) (label string, marked bool, err error) {
	if format == "png" && len(raw) < 20 || format == "jpeg" && len(raw) < 2 {
		return "", false, mobile.Invalid("invalid image metadata")
	}
	chunks := [][]byte{}
	if format == "png" {
		for pos := 8; pos+12 <= len(raw); {
			n := int(binary.BigEndian.Uint32(raw[pos : pos+4]))
			if n < 0 || n > len(raw)-pos-12 {
				return "", false, mobile.Invalid("invalid PNG metadata")
			}
			kind := string(raw[pos+4 : pos+8])
			if kind == "tEXt" || kind == "iTXt" || kind == "zTXt" {
				text, e := pngMetadataText(kind, raw[pos+8:pos+8+n])
				if e != nil {
					return "", false, e
				}
				chunks = append(chunks, text)
			}
			pos += 12 + n
		}
	} else {
		for pos := 2; pos+4 <= len(raw); {
			if raw[pos] != 0xff {
				break
			}
			marker := raw[pos+1]
			if marker == 0xda || marker == 0xd9 {
				break
			}
			if marker == 0xff {
				pos++
				continue
			}
			if marker == 0x01 || marker >= 0xd0 && marker <= 0xd7 {
				pos += 2
				continue
			}
			n := int(binary.BigEndian.Uint16(raw[pos+2 : pos+4]))
			if n < 2 || n > len(raw)-pos-2 {
				return "", false, mobile.Invalid("invalid JPEG metadata")
			}
			if marker >= 0xe0 && marker <= 0xef || marker == 0xfe {
				chunks = append(chunks, raw[pos+4:pos+2+n])
			}
			pos += 2 + n
		}
	}
	for _, chunk := range chunks {
		if !bytes.Contains(bytes.ToUpper(chunk), []byte("AIGC")) {
			continue
		}
		marked = true
		// XML escaping is decoded without resolving entities, URLs, DTDs or network resources.
		s := html.UnescapeString(string(chunk))
		loc := regexp.MustCompile(`\{\s*"AIGC"\s*:`).FindStringIndex(s)
		if loc == nil {
			continue
		}
		start := loc[0]
		if len(s)-start > 16384 {
			s = s[:start+16384]
		}
		var v mobile.AIGCLabel
		decoder := json.NewDecoder(strings.NewReader(s[start:]))
		decoder.DisallowUnknownFields()
		if decoder.Decode(&v) != nil || !v.Valid() {
			continue
		}
		b, e := json.Marshal(v)
		if e != nil {
			return "", true, e
		}
		if label != "" && label != string(b) {
			return "", true, mobile.Invalid("conflicting AIGC labels")
		}
		label = string(b)
	}
	if marked && label == "" {
		return "", true, mobile.Invalid("unreadable AIGC marker; use a source image with intact metadata")
	}
	return
}

func pngMetadataText(kind string, data []byte) ([]byte, error) {
	i := bytes.IndexByte(data, 0)
	if i < 0 {
		return nil, mobile.Invalid("invalid PNG text")
	}
	keyword := data[:i]
	text := data[i+1:]
	compressed := false
	if kind == "zTXt" {
		if len(text) < 1 || text[0] != 0 {
			return nil, mobile.Invalid("invalid PNG compressed text")
		}
		text = text[1:]
		compressed = true
	}
	if kind == "iTXt" {
		if len(text) < 2 || text[0] > 1 || text[1] != 0 {
			return nil, mobile.Invalid("invalid PNG international text")
		}
		compressed = text[0] == 1
		text = text[2:]
		for n := 0; n < 2; n++ {
			j := bytes.IndexByte(text, 0)
			if j < 0 {
				return nil, mobile.Invalid("invalid PNG text field")
			}
			text = text[j+1:]
		}
	}
	if compressed {
		reader, e := zlib.NewReader(bytes.NewReader(text))
		if e != nil {
			return nil, mobile.Invalid("invalid compressed metadata")
		}
		defer reader.Close()
		text, e = io.ReadAll(io.LimitReader(reader, 16385))
		if e != nil {
			return nil, mobile.Invalid("invalid compressed metadata")
		}
	}
	if len(text) > 16384 {
		return nil, mobile.Invalid("metadata exceeds supported safe limit")
	}
	out := append(append([]byte{}, keyword...), 0)
	return append(out, text...), nil
}

func embedAIGC(raw []byte, mime, label string) ([]byte, error) {
	if label == "" {
		return raw, nil
	}
	var v mobile.AIGCLabel
	if json.Unmarshal([]byte(label), &v) != nil || !v.Valid() {
		return nil, mobile.Invalid("invalid AIGC label")
	}
	if mime == "image/png" {
		payload := append([]byte("AIGC\x00"), []byte(label)...)
		chunk := make([]byte, 12+len(payload))
		binary.BigEndian.PutUint32(chunk[:4], uint32(len(payload)))
		copy(chunk[4:8], "tEXt")
		copy(chunk[8:], payload)
		binary.BigEndian.PutUint32(chunk[8+len(payload):], crc32.ChecksumIEEE(chunk[4:8+len(payload)]))
		if len(raw) < 33 {
			return nil, mobile.Invalid("invalid PNG")
		}
		out := append([]byte{}, raw[:33]...)
		out = append(out, chunk...)
		return append(out, raw[33:]...), nil
	}
	if len(raw) < 2 || len(label) > 60000 {
		return nil, mobile.Invalid("invalid JPEG")
	}
	payload := append([]byte("AIGC\x00"), []byte(label)...)
	segment := []byte{0xff, 0xfe, 0, 0}
	binary.BigEndian.PutUint16(segment[2:], uint16(len(payload)+2))
	segment = append(segment, payload...)
	out := append([]byte{}, raw[:2]...)
	out = append(out, segment...)
	return append(out, raw[2:]...), nil
}
