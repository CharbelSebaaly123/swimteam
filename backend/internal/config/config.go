package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	Port            string
	DBPath          string
	JWTSecret       string
	JWTExpiration   time.Duration
	CORSOrigins     []string
	AdminUsername   string
	AdminPassword   string
	AdminEmail      string
	PhotoMaxDim     int
	PhotoJPEGQuality float64
}

func Load() Config {
	return Config{
		Port:             getenv("PORT", "8080"),
		DBPath:           getenv("DB_PATH", "swimteam.db"),
		JWTSecret:        getenv("JWT_SECRET", "swimteamgo-dev-secret-key-change-me-32b"),
		JWTExpiration:    time.Duration(getenvInt("JWT_EXPIRATION_MS", 86400000)) * time.Millisecond,
		CORSOrigins:      []string{"http://localhost:5173", "http://127.0.0.1:5173"},
		AdminUsername:    getenv("ADMIN_USERNAME", "admin"),
		AdminPassword:    getenv("ADMIN_PASSWORD", "admin123"),
		AdminEmail:       getenv("ADMIN_EMAIL", "coach@swimteam.local"),
		PhotoMaxDim:      getenvInt("PHOTO_MAX_DIMENSION", 800),
		PhotoJPEGQuality: float64(getenvInt("PHOTO_JPEG_QUALITY", 75)) / 100.0,
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func getenvInt(key string, fallback int) int {
	if v := os.Getenv(key); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return fallback
}
