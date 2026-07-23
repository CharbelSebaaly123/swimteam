package main

import (
	"log"
	"net/http"
	"os"
	"path/filepath"
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
	chimw "github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
)

func main() {
	cfg := config.Load()
	gdb, err := db.Open(cfg.DBPath)
	if err != nil {
		log.Fatalf("db: %v", err)
	}

	sampleDir := os.Getenv("SAMPLE_DATA_DIR")
	if sampleDir == "" {
		sampleDir = findSampleDir()
	}
	if err := seed.Run(gdb, cfg, sampleDir); err != nil {
		log.Fatalf("seed: %v", err)
	}

	tokens := auth.NewTokenService(cfg.JWTSecret, cfg.JWTExpiration)
	svc := &services.Service{DB: gdb, Tokens: tokens, Cfg: cfg}
	api := &handlers.API{Svc: svc}

	r := chi.NewRouter()
	r.Use(chimw.RequestID)
	r.Use(chimw.RealIP)
	r.Use(chimw.Logger)
	r.Use(chimw.Recoverer)
	r.Use(chimw.Timeout(60 * time.Second))
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   cfg.CORSOrigins,
		AllowedMethods:   []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type"},
		AllowCredentials: true,
	}))

	r.Get("/api/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"status":"ok"}`))
	})

	r.Route("/api/auth", func(r chi.Router) {
		r.Post("/signup", api.Signup)
		r.Post("/login", api.Login)
		r.With(middleware.Authenticate(tokens)).Post("/change-password", api.ChangePassword)
	})

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
		r.Get("/me", api.GetCoachMe)
		r.Put("/me", api.UpdateCoachMe)
		r.Post("/me/photo", api.UploadCoachPhoto)
		r.Get("/me/photo", api.GetCoachPhoto)
		r.Get("/members", api.ListMembers)
		r.Get("/members/{userId}", api.GetMember)
		r.Get("/members/{userId}/photo", api.GetMemberPhoto)
		r.Get("/metrics", api.Metrics)
		r.Get("/reports/age-groups", api.AgeGroups)
	})

	addr := ":" + cfg.Port
	log.Printf("SwimTeam Go API listening on %s (db=%s)", addr, cfg.DBPath)
	if err := http.ListenAndServe(addr, r); err != nil {
		log.Fatal(err)
	}
}

func findSampleDir() string {
	candidates := []string{
		"sample-data",
		filepath.Join("..", "sample-data"),
		filepath.Join("backend", "sample-data"),
	}
	for _, c := range candidates {
		if st, err := os.Stat(c); err == nil && st.IsDir() {
			return c
		}
	}
	return "sample-data"
}
