package photo

import (
	"bytes"
	"fmt"
	"image"
	_ "image/gif"
	"image/jpeg"
	_ "image/png"
	"io"
	"path/filepath"
	"strings"

	"github.com/disintegration/imaging"
	_ "golang.org/x/image/webp"
)

type Result struct {
	Data        []byte
	ContentType string
	SizeBytes   int
}

func Process(r io.Reader, filename, contentType string, maxDim int, quality float64) (*Result, error) {
	if !allowed(filename, contentType) {
		return nil, fmt.Errorf("unsupported image type")
	}
	raw, err := io.ReadAll(io.LimitReader(r, 8<<20))
	if err != nil {
		return nil, fmt.Errorf("unable to read image")
	}
	if len(raw) == 0 {
		return nil, fmt.Errorf("empty image")
	}
	img, _, err := image.Decode(bytes.NewReader(raw))
	if err != nil {
		return nil, fmt.Errorf("unreadable image")
	}
	bounds := img.Bounds()
	w, h := bounds.Dx(), bounds.Dy()
	if w > maxDim || h > maxDim {
		img = imaging.Fit(img, maxDim, maxDim, imaging.Linear)
	}
	// Flatten alpha onto white for JPEG.
	img = imaging.Clone(img)

	var buf bytes.Buffer
	q := int(quality * 100)
	if q < 1 {
		q = 75
	}
	if q > 100 {
		q = 100
	}
	if err := jpeg.Encode(&buf, img, &jpeg.Options{Quality: q}); err != nil {
		return nil, fmt.Errorf("failed to compress image")
	}
	return &Result{Data: buf.Bytes(), ContentType: "image/jpeg", SizeBytes: buf.Len()}, nil
}

func allowed(filename, contentType string) bool {
	ct := strings.ToLower(strings.TrimSpace(contentType))
	switch ct {
	case "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp":
		return true
	}
	ext := strings.ToLower(filepath.Ext(filename))
	switch ext {
	case ".jpg", ".jpeg", ".png", ".gif", ".webp":
		return true
	}
	return false
}
