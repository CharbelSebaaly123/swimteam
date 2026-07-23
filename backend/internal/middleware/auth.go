package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/CharbelSebaaly123/swimteam/backend/internal/auth"
	"github.com/CharbelSebaaly123/swimteam/backend/internal/models"
)

type ctxKey string

const UserClaimsKey ctxKey = "claims"

func Authenticate(tokens *auth.TokenService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			header := r.Header.Get("Authorization")
			if !strings.HasPrefix(header, "Bearer ") {
				writeJSON(w, http.StatusUnauthorized, map[string]string{"message": "Unauthorized"})
				return
			}
			claims, err := tokens.Parse(strings.TrimPrefix(header, "Bearer "))
			if err != nil {
				writeJSON(w, http.StatusUnauthorized, map[string]string{"message": "Invalid or expired token"})
				return
			}
			ctx := context.WithValue(r.Context(), UserClaimsKey, claims)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func RequireRole(roles ...models.Role) func(http.Handler) http.Handler {
	allowed := map[models.Role]bool{}
	for _, role := range roles {
		allowed[role] = true
	}
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			claims, ok := ClaimsFrom(r.Context())
			if !ok || !allowed[claims.Role] {
				writeJSON(w, http.StatusForbidden, map[string]string{"message": "Forbidden"})
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

func ClaimsFrom(ctx context.Context) (*auth.Claims, bool) {
	c, ok := ctx.Value(UserClaimsKey).(*auth.Claims)
	return c, ok
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = jsonEncode(w, body)
}
