package handlers_test

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/CharbelSebaaly123/swimteam/backend/internal/auth"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/config"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/db"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/handlers"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/middleware"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/models"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/seed"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/services"
	"github.com/go-chi/chi/v5"
	"github.com/stretchr/testify/require"
)

func findSampleDir(t *testing.T) string {
	t.Helper()
	candidates := []string{
		filepath.Join("..", "..", "sample-data"),
		filepath.Join("..", "sample-data"),
		"sample-data",
	}
	for _, c := range candidates {
		if st, err := os.Stat(c); err == nil && st.IsDir() {
			return c
		}
	}
	t.Fatalf("sample-data directory not found")
	return ""
}

func setupRouter(t *testing.T) http.Handler {
	t.Helper()
	dir := t.TempDir()
	dbPath := filepath.Join(dir, "test.db")
	cfg := config.Load()
	cfg.DBPath = dbPath
	cfg.JWTSecret = "test-secret-key-at-least-32-bytes!!"
	cfg.JWTExpiration = time.Hour
	cfg.PhotoMaxDim = 800
	cfg.PhotoJPEGQuality = 0.75

	gdb, err := db.Open(dbPath)
	require.NoError(t, err)

	sampleDir := findSampleDir(t)
	require.NoError(t, seed.Run(gdb, cfg, sampleDir))

	tokens := auth.NewTokenService(cfg.JWTSecret, cfg.JWTExpiration)
	svc := &services.Service{DB: gdb, Tokens: tokens, Cfg: cfg}
	api := &handlers.API{Svc: svc}

	r := chi.NewRouter()
	r.Post("/api/auth/signup", api.Signup)
	r.Post("/api/auth/login", api.Login)
	r.With(middleware.Authenticate(tokens)).Post("/api/auth/change-password", api.ChangePassword)

	r.Route("/api/profiles", func(r chi.Router) {
		r.Use(middleware.Authenticate(tokens))
		r.Use(middleware.RequireRole(models.RoleMember, models.RoleCoach))
		r.Get("/me", api.GetMyProfile)
		r.Put("/me", api.UpdateMyProfile)
		r.Post("/me/photo", api.UploadMyPhoto)
		r.Get("/me/photo", api.GetMyPhoto)
		r.Delete("/me/photo", api.DeleteMyPhoto)
	})

	r.Route("/api/coach", func(r chi.Router) {
		r.Use(middleware.Authenticate(tokens))
		r.Use(middleware.RequireRole(models.RoleCoach))
		r.Get("/members", api.ListMembers)
		r.Get("/members/{userId}", api.GetMember)
		r.Get("/metrics", api.Metrics)
		r.Get("/reports/age-groups", api.AgeGroups)
		r.Get("/me", api.GetCoachMe)
	})
	return r
}

func doJSON(t *testing.T, h http.Handler, method, path, token string, body any) (int, map[string]any) {
	t.Helper()
	var reader io.Reader
	if body != nil {
		b, err := json.Marshal(body)
		require.NoError(t, err)
		reader = bytes.NewReader(b)
	}
	req := httptest.NewRequest(method, path, reader)
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}
	rr := httptest.NewRecorder()
	h.ServeHTTP(rr, req)
	var out map[string]any
	_ = json.Unmarshal(rr.Body.Bytes(), &out)
	return rr.Code, out
}

func TestAuthAccessAndCoachFlows(t *testing.T) {
	h := setupRouter(t)

	// Login default coach
	code, coach := doJSON(t, h, "POST", "/api/auth/login", "", map[string]string{
		"username": "admin", "password": "admin123",
	})
	require.Equal(t, 200, code)
	require.Equal(t, "COACH", coach["role"])
	coachToken := coach["token"].(string)

	// Login sample member
	code, sample := doJSON(t, h, "POST", "/api/auth/login", "", map[string]string{
		"username": "sample", "password": "sample123",
	})
	require.Equal(t, 200, code)
	require.Equal(t, "MEMBER", sample["role"])
	memberToken := sample["token"].(string)

	// Member can read own profile
	code, profile := doJSON(t, h, "GET", "/api/profiles/me", memberToken, nil)
	require.Equal(t, 200, code)
	require.Equal(t, true, profile["profileCompleted"])
	require.Equal(t, "Maroun Labib", profile["firstName"])

	// Coach cannot use member profile endpoint
	code, denied := doJSON(t, h, "GET", "/api/profiles/me", coachToken, nil)
	require.Equal(t, 403, code)
	require.Contains(t, denied["message"], "Only team members")

	// Member cannot access coach roster
	code, _ = doJSON(t, h, "GET", "/api/coach/members", memberToken, nil)
	require.Equal(t, 403, code)

	// Coach sees roster + metrics + age groups
	code, _ = doJSON(t, h, "GET", "/api/coach/members?sort=age&direction=asc", coachToken, nil)
	require.Equal(t, 200, code)
	code, metrics := doJSON(t, h, "GET", "/api/coach/metrics", coachToken, nil)
	require.Equal(t, 200, code)
	require.GreaterOrEqual(t, int(metrics["totalMembers"].(float64)), 1)
	code, _ = doJSON(t, h, "GET", "/api/coach/reports/age-groups", coachToken, nil)
	require.Equal(t, 200, code)

	// Signup new member and update profile
	code, newbie := doJSON(t, h, "POST", "/api/auth/signup", "", map[string]string{
		"username": "swimmer1", "email": "s1@example.com", "password": "secret12",
	})
	require.Equal(t, 201, code)
	newToken := newbie["token"].(string)
	code, _ = doJSON(t, h, "PUT", "/api/profiles/me", newToken, map[string]any{
		"firstName": "Ada", "lastName": "Lovelace", "nickname": "Ada",
		"email": "s1@example.com", "phone": "+14155550001",
		"dateOfBirth": "2005-06-01", "address": "",
		"emergencyContactName": "Mom", "emergencyContactPhone": "+14155550002",
		"strokeSpecialty": "Butterfly",
	})
	require.Equal(t, 200, code)

	// Change password
	code, _ = doJSON(t, h, "POST", "/api/auth/change-password", newToken, map[string]string{
		"currentPassword": "secret12", "newPassword": "secret99",
	})
	require.Equal(t, 200, code)
	code, _ = doJSON(t, h, "POST", "/api/auth/login", "", map[string]string{
		"username": "swimmer1", "password": "secret99",
	})
	require.Equal(t, 200, code)
}

func TestPhotoUploadResizes(t *testing.T) {
	h := setupRouter(t)
	_, sample := doJSON(t, h, "POST", "/api/auth/login", "", map[string]string{
		"username": "sample", "password": "sample123",
	})
	token := sample["token"].(string)

	// Create a simple PNG larger than max dim via imaging is overkill; upload sample jpg again.
	samplePath := filepath.Join(findSampleDir(t), "sample-member.jpg")
	f, err := os.Open(samplePath)
	require.NoError(t, err)
	defer f.Close()

	var buf bytes.Buffer
	mw := multipart.NewWriter(&buf)
	fw, err := mw.CreateFormFile("file", "sample-member.jpg")
	require.NoError(t, err)
	_, err = io.Copy(fw, f)
	require.NoError(t, err)
	require.NoError(t, mw.Close())

	req := httptest.NewRequest("POST", "/api/profiles/me/photo", &buf)
	req.Header.Set("Content-Type", mw.FormDataContentType())
	req.Header.Set("Authorization", "Bearer "+token)
	rr := httptest.NewRecorder()
	h.ServeHTTP(rr, req)
	require.Equal(t, 200, rr.Code, rr.Body.String())

	var resp handlers.PhotoUploadResponse
	require.NoError(t, json.Unmarshal(rr.Body.Bytes(), &resp))
	require.True(t, resp.HasPhoto)
	require.Equal(t, "image/jpeg", resp.ContentType)
	require.Greater(t, resp.SizeBytes, 0)
	fmt.Printf("uploaded size=%d\n", resp.SizeBytes)
}
