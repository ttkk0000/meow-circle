package mobileapi

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"image"
	"image/jpeg"
	"image/png"
	"io"
	"kitty-circle/internal/mediablob"
	"net/http"
	"path/filepath"
	"strconv"

	"kitty-circle/internal/mobile"
)

const maxImageBytes = 10 << 20

func (h *Handler) upload(w http.ResponseWriter, r *http.Request, actor int64) (any, error) {
	return h.uploadFor(w, r, actor, "")
}
func (h *Handler) uploadFor(w http.ResponseWriter, r *http.Request, actor int64, safetyToken string) (any, error) {
	if safetyToken != "" {
		if e := h.Service.ValidateSafetySession(r.Context(), safetyToken); e != nil {
			return nil, e
		}
	}
	if key := r.Header.Get("Idempotency-Key"); len(key) < 8 || len(key) > 100 {
		return nil, mobile.Invalid("idempotency key required")
	}
	if r.Method != http.MethodPost {
		return nil, mobile.Invalid("upload requires POST")
	}
	if actor <= 0 && safetyToken == "" {
		return nil, mobile.ErrUnauthorized
	}
	select {
	case h.uploadSlots <- struct{}{}:
		defer func() { <-h.uploadSlots }()
	default:
		return nil, mobile.ErrRateLimited
	}
	r.Body = http.MaxBytesReader(w, r.Body, maxImageBytes+(1<<20))
	if e := r.ParseMultipartForm(1 << 20); e != nil {
		return nil, mobile.Invalid("image exceeds 10 MiB or invalid multipart body")
	}
	if r.MultipartForm != nil {
		defer r.MultipartForm.RemoveAll()
	}
	file, _, e := r.FormFile("file")
	if e != nil {
		return nil, mobile.Invalid("file required")
	}
	defer file.Close()
	scope := r.FormValue("scope")
	if safetyToken != "" {
		scope = "private"
	}
	if scope == "" {
		scope = "private"
	}
	if scope != "public" && scope != "private" {
		return nil, mobile.Invalid("scope must be public or private")
	}
	raw, e := io.ReadAll(io.LimitReader(file, maxImageBytes+1))
	if e != nil {
		return nil, e
	}
	if len(raw) > maxImageBytes {
		return nil, mobile.Invalid("image exceeds 10 MiB")
	}
	config, format, e := image.DecodeConfig(bytes.NewReader(raw))
	if e != nil || (format != "jpeg" && format != "png") || config.Width < 1 || config.Height < 1 || int64(config.Width)*int64(config.Height) > 20000000 {
		return nil, mobile.Invalid("use JPEG/PNG up to 20 megapixels")
	}
	img, _, e := image.Decode(bytes.NewReader(raw))
	if e != nil {
		return nil, mobile.Invalid("invalid image")
	}
	sourceLabel, knownAI, e := extractAIGC(raw, format)
	if e != nil {
		return nil, e
	}
	if knownAI {
		sourceLabel, e = mobile.PropagationLabel(sourceLabel, h.Service.Propagator, mobile.Fingerprint(string(raw)), "3")
		if e != nil {
			return nil, e
		}
	}
	var encoded bytes.Buffer
	mime := "image/jpeg"
	if format == "png" {
		mime = "image/png"
		e = png.Encode(&encoded, img)
	} else {
		e = jpeg.Encode(&encoded, img, &jpeg.Options{Quality: 90})
	}
	if e != nil {
		return nil, e
	}
	if encoded.Len() > maxImageBytes {
		return nil, mobile.Invalid("processed image exceeds 10 MiB")
	}
	sum := sha256.Sum256(encoded.Bytes())
	digest := hex.EncodeToString(sum[:])
	metadata := mobile.MediaFile{Scope: scope, MIME: mime, Size: int64(encoded.Len()), SHA256: digest, Width: config.Width, Height: config.Height, KnownAI: knownAI, SourceLabel: sourceLabel}
	record, e := h.Service.ReserveMedia(r.Context(), actor, safetyToken, r.Header.Get("Idempotency-Key"), metadata)
	if e != nil {
		return nil, e
	}
	if record.State == "ready" {
		return mediaResult(record, safetyToken != "")
	}
	reserved, e := mobile.Decode[mobile.MediaFile](record)
	if e != nil {
		return nil, e
	}
	filename := reserved.Filename
	if filename == "" || filepath.Base(filename) != filename {
		return nil, mobile.ErrForbidden
	}

	if e = h.blobs().Put(r.Context(), filename, encoded.Bytes()); e != nil {
		return nil, e
	}
	record, e = h.Service.CompleteReservedMedia(r.Context(), actor, safetyToken, record.ID, record.Version)
	if e != nil {
		return nil, e
	}
	return mediaResult(record, safetyToken != "")
}
func mediaResult(record mobile.Record, safety bool) (any, error) {
	var public map[string]any
	if e := decodeData(record.Data, &public); e != nil {
		return nil, e
	}
	delete(public, "filename")
	delete(public, "upload_lease_until")
	url := "/api/v2/media/"
	if safety {
		url = "/api/v2/safety/media/"
	}
	return map[string]any{"id": record.ID, "owner_id": record.OwnerID, "state": record.State, "version": record.Version, "data": public, "url": url + strconv.FormatInt(record.ID, 10) + "/content"}, nil
}

func (h *Handler) serveMedia(w http.ResponseWriter, r *http.Request, actor, id int64) error {
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		return mobile.Invalid("content requires GET")
	}
	file, e := h.Service.MediaFile(r.Context(), actor, id)
	if op, allowed := h.operator(r, "evidence_reader"); allowed {
		file, e = h.Service.StaffMediaFile(mobile.WithOperator(r.Context(), op.ID), id)
	}
	if e != nil {
		return e
	}
	return h.serveMediaFile(w, r, file)
}
func (h *Handler) serveMediaFile(w http.ResponseWriter, r *http.Request, file mobile.MediaFile) error {
	if filepath.Base(file.Filename) != file.Filename || file.Filename == "" {
		return mobile.ErrNotFound
	}
	raw, modified, e := h.blobs().Read(r.Context(), file.Filename)
	if errors.Is(e, mediablob.ErrNotFound) {
		return mobile.ErrNotFound
	}
	if e != nil {
		return e
	}
	checksum := sha256.Sum256(raw)
	if int64(len(raw)) != file.Size || hex.EncodeToString(checksum[:]) != file.SHA256 {
		return mobile.ErrUnavailable
	}
	w.Header().Set("Content-Type", file.MIME)
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Cache-Control", "private, no-store")
	if file.SourceLabel != "" {
		marked, e := embedAIGC(raw, file.MIME, file.SourceLabel)
		if e != nil {
			return e
		}
		w.Header().Set("Content-Type", file.MIME)
		http.ServeContent(w, r, "image", modified, bytes.NewReader(marked))
		return nil
	}
	http.ServeContent(w, r, "image", modified, bytes.NewReader(raw))
	return nil
}
