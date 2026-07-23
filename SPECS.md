# Swim Team Profile Application — Spec Plan

Responsive web app for swim team members to enter personal information, with coach admin access to all profiles, metrics, and completion status.

**Stack:** Java Spring Boot (REST) + React (Vite)  
**Roles:** `COACH` (admin), `MEMBER`  
**Default coach:** username `admin` / password `admin123`

---

## Spec 1 — Backend foundation & domain model ✅

- Spring Boot 3 project under `backend/`
- Entities: `User` (auth + role), `MemberProfile` (personal info + metrics)
- H2 in-memory DB for local/dev
- JPA repositories
- Application config (`application.yml`)

## Spec 2 — Auth, JWT security, default admin ✅

- Member signup (`POST /api/auth/signup`)
- Login (`POST /api/auth/login`) → JWT
- Spring Security: `MEMBER` → own profile only; `COACH` → all
- Seed default coach user `admin` / `admin123` on startup

## Spec 3 — Member profile REST APIs ✅

- `GET /api/profiles/me` — current member profile
- `PUT /api/profiles/me` — create/update own profile
- Profile fields + `profileCompleted` flag

## Spec 4 — Coach roster & metrics endpoints ✅

- `GET /api/coach/members` — all members + completion flag
- `GET /api/coach/members/{id}` — full profile
- `GET /api/coach/metrics` — aggregate metrics

## Spec 5 — React frontend foundation & auth ✅

- Vite + React under `frontend/`
- Login, signup, role-based routing, JWT auth context

## Spec 6 — Member profile UI ✅

- Responsive profile form with personal info + metrics
- Completion status banner

## Spec 7 — Coach dashboard UI ✅

- Metrics strip, roster with completion flags, member detail view

## Spec 8 — Docs & verification ✅

- Root README with run instructions
- CORS + Vite proxy
- Smoke-tested API flows
