# Swim Team Profile Application (Go) — Spec Plan

Responsive web app for swim team members to enter personal information, with coach admin access to all profiles, metrics, and completion status.

**Stack:** Go (REST) + React (Vite)  
**Roles:** `COACH` (admin), `MEMBER`  
**Default coach:** username `admin` / password `admin123`  
**Database:** SQLite (local/dev file DB)

---

## Spec 1 — Backend foundation & domain model ✅

- Go module under `backend/` with Chi router and GORM
- Models: `User` (auth + role), `MemberProfile` (personal info + metrics + photo)
- SQLite database for local/dev
- App config via environment / defaults
- Project layout suitable for juniors (`cmd/`, `internal/`)

## Spec 2 — Auth, JWT security, default admin ✅

- Member signup (`POST /api/auth/signup`)
- Login (`POST /api/auth/login`) → JWT
- Change password (`POST /api/auth/change-password`) for coach and member
- Middleware: `MEMBER` → own profile only; `COACH` → all coach APIs
- Seed default coach `admin` / `admin123` (Labib Waked, nickname Wahsh) on startup
- Seed sample member `sample` / `sample123` (Maroun Labib Waked) with photo

## Spec 3 — Member profile REST APIs ✅

- `GET/PUT /api/profiles/me` — own profile
- Required: email, international phone (E.164), date of birth
- Optional: address, nickname, metrics
- `profileCompleted` flag computed server-side
- Photo upload/download/delete with resize + JPEG compression

## Spec 4 — Coach roster, metrics & reports ✅

- Coach own profile + photo endpoints
- `GET /api/coach/members` — roster + completion flags; sort by age/name/completed/username
- `GET /api/coach/members/{id}` — full profile (+ photo)
- `GET /api/coach/metrics` — aggregate metrics
- `GET /api/coach/reports/age-groups` — members grouped by age brackets

## Spec 5 — React frontend foundation & auth ✅

- Vite + React under `frontend/`
- Login, signup, change-password, role-based routing, JWT auth context
- Responsive layout; Vite proxies `/api` → `:8080`

## Spec 6 — Member profile UI ✅

- Responsive profile form (personal info + metrics + photo)
- Completion status banner
- Client-side image resize before upload

## Spec 7 — Coach dashboard UI ✅

- Metrics strip, sortable roster (default age), age-group report
- Coach profile page (name, nickname, photo)
- Member detail view

## Spec 8 — Docs, tests & verification ✅

- Architecture + backend/frontend design docs (REST, JWT, Go & React best practices)
- Testing guide for juniors + automated Go API tests
- README with run instructions
- Smoke-tested API flows
