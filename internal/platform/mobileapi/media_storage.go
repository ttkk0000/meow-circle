package mobileapi

import (
	"fmt"
	"os"

	"kitty-circle/internal/mediablob"
)

func (h *Handler) blobs() mediablob.Storage {
	if h.MediaStorage != nil {
		return h.MediaStorage
	}
	return mediablob.Disk{Dir: h.MediaDir}
}
func (h *Handler) ConfigureMediaStorage() error {
	if dir := os.Getenv("MND_MOBILE_MEDIA_DIR"); dir != "" {
		h.MediaDir = dir
	}
	switch os.Getenv("MND_MEDIA_BACKEND") {
	case "", "local":
		return nil
	case "s3":
		storage, err := mediablob.S3FromEnvironment()
		if err != nil {
			return fmt.Errorf("invalid private S3 media configuration")
		}
		h.MediaStorage = storage
		return nil
	default:
		return fmt.Errorf("unsupported MND_MEDIA_BACKEND")
	}
}
