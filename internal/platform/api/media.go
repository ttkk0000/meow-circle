package api

import (
	"crypto/rand"
	"encoding/hex"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"bestTry/internal/domain"
)

const (
	maxImageBytes = 10 * 1024 * 1024      // 10 MB
	maxVideoBytes = 200 * 1024 * 1024     // 200 MB
	maxUploadRead = maxVideoBytes + 1<<20 // reserve 1 MB for form overhead
)

var (
	allowedImageMIMEs = map[string]string{
		"image/jpeg": ".jpg",
		"image/png":  ".png",
		"image/webp": ".webp",
		"image/gif":  ".gif",
	}
	allowedVideoMIMEs = map[string]string{
		"video/mp4":       ".mp4",
		"video/webm":      ".webm",
		"video/quicktime": ".mov",
	}
)

func (r *Router) handleMedia(w http.ResponseWriter, req *http.Request) {
	switch req.Method {
	case http.MethodGet:
		user, _ := currentUser(req)
		writeOK(w, map[string]any{"items": r.store.ListMediaByOwner(user.ID)})
	case http.MethodPost:
		r.uploadMedia(w, req)
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func (r *Router) uploadMedia(w http.ResponseWriter, req *http.Request) {
	user, _ := currentUser(req)

	req.Body = http.MaxBytesReader(w, req.Body, maxUploadRead)
	if err := req.ParseMultipartForm(32 << 20); err != nil {
		writeError(w, http.StatusBadRequest, "invalid multipart form or payload too large")
		return
	}

	file, header, err := req.FormFile("file")
	if err != nil {
		writeError(w, http.StatusBadRequest, "file field is required")
		return
	}
	defer func() { _ = file.Close() }()

	buf := make([]byte, 512)
	n, _ := file.Read(buf)
	mime := http.DetectContentType(buf[:n])
	if _, err := file.Seek(0, io.SeekStart); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to rewind upload")
		return
	}

	kind, ext, ok := classifyMIME(mime)
	if !ok {
		writeError(w, http.StatusUnsupportedMediaType, "unsupported media type: "+mime)
		return
	}

	var maxSize int64 = maxImageBytes
	if kind == domain.MediaKindVideo {
		maxSize = maxVideoBytes
	}
	if header.Size > maxSize {
		writeError(w, http.StatusRequestEntityTooLarge, "file too large")
		return
	}

	if ext == "" {
		ext = filepath.Ext(header.Filename)
	}

	filename, err := randomFilename(ext)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to allocate filename")
		return
	}

	uploadDir := r.uploadsDir()
	if err := os.MkdirAll(uploadDir, 0o755); err != nil {
		writeError(w, http.StatusInternalServerError, "failed to prepare storage")
		return
	}
	target := filepath.Join(uploadDir, filename)
	out, err := os.Create(target)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "failed to create file")
		return
	}
	size, err := io.Copy(out, file)
	_ = out.Close()
	if err != nil {
		_ = os.Remove(target)
		writeError(w, http.StatusInternalServerError, "failed to save file")
		return
	}

	// Auto-approve by default; integrate a real moderation service here if desired.
	status := domain.MediaStatusApproved

	media := r.store.CreateMedia(domain.Media{
		OwnerID:  user.ID,
		Kind:     kind,
		MIME:     mime,
		Size:     size,
		Filename: filename,
		URL:      "/uploads/" + filename,
		Status:   status,
	})
	writeCreated(w, media)
}

func (r *Router) handleMediaChild(w http.ResponseWriter, req *http.Request) {
	id, err := parseID(req.URL.Path, "/api/v1/media/")
	if err != nil {
		writeError(w, http.StatusBadRequest, "invalid media id")
		return
	}

	switch req.Method {
	case http.MethodGet:
		m, ok := r.store.GetMedia(id)
		if !ok {
			writeError(w, http.StatusNotFound, "media not found")
			return
		}
		writeOK(w, m)
	case http.MethodDelete:
		claims, ok := r.parseAuth(req)
		if !ok {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		m, ok := r.store.GetMedia(id)
		if !ok {
			writeError(w, http.StatusNotFound, "media not found")
			return
		}
		if m.OwnerID != claims.UserID {
			writeError(w, http.StatusForbidden, "not media owner")
			return
		}
		r.removeMediaFile(m)
		r.store.DeleteMedia(id)
		writeOK(w, map[string]any{"deleted": true, "id": id})
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

func classifyMIME(mime string) (domain.MediaKind, string, bool) {
	mime = strings.ToLower(strings.TrimSpace(mime))
	if ext, ok := allowedImageMIMEs[mime]; ok {
		return domain.MediaKindImage, ext, true
	}
	if ext, ok := allowedVideoMIMEs[mime]; ok {
		return domain.MediaKindVideo, ext, true
	}
	return "", "", false
}

func randomFilename(ext string) (string, error) {
	b := make([]byte, 16)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	if ext != "" && !strings.HasPrefix(ext, ".") {
		ext = "." + ext
	}
	return hex.EncodeToString(b) + ext, nil
}

func (r *Router) uploadsDir() string {
	return filepath.Join(getWorkingDir(), "data", "uploads")
}

func (r *Router) removeMediaFile(m domain.Media) {
	if m.Filename == "" {
		return
	}
	path := filepath.Join(r.uploadsDir(), m.Filename)
	_ = os.Remove(path)
}
