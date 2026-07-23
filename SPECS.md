# Swim Team Profile Application — Spec Plan

Responsive web app for swim team members to enter personal information, with coach admin access to all profiles, metrics, and completion status.

**Stack:** Java Spring Boot (REST) + React (Vite)  
**Roles:** `COACH` (admin), `MEMBER`  
**Default coach:** username `admin` / password `admin123`

---

## Spec 1 — Backend foundation & domain model

- Spring Boot 3 project under `backend/`
- Entities: `User` (auth + role), `MemberProfile` (personal info + metrics)
- H2 in-memory DB for local/dev
- JPA repositories
- Application config (`application.yml`)

**Done when:** Backend builds; schema creates on startup.

---

## Spec 2 — Auth, JWT security, default admin

- Member signup (`POST /api/auth/signup`)
- Login (`POST /api/auth/login`) → JWT
- Spring Security: `MEMBER` → own profile only; `COACH` → all
- Seed default coach user `admin` / `admin123` on startup

**Done when:** Signup/login work; JWT protects endpoints; admin exists.

---

## Spec 3 — Member profile REST APIs

- `GET /api/profiles/me` — current member profile
- `PUT /api/profiles/me` — create/update own profile
- Profile fields: name, email, phone, date of birth, emergency contact, address, stroke specialty, personal bests / metrics
- `profileCompleted` flag derived from required fields

**Done when:** Member can CRUD own profile; cannot access others.

---

## Spec 4 — Coach roster & metrics endpoints

- `GET /api/coach/members` — all members + completion flag
- `GET /api/coach/members/{id}` — full profile
- `GET /api/coach/metrics` — aggregate metrics (counts, completion rate)
- Coach-only authorization

**Done when:** Coach endpoints return roster, profiles, and metrics.

---

## Spec 5 — React frontend foundation & auth

- Vite + React app under `frontend/`
- Routes: login, signup, member area, coach area
- Auth context + JWT storage
- Responsive shell / navigation by role

**Done when:** Login/signup UI works against backend.

---

## Spec 6 — Member profile UI

- Responsive profile form (personal info + metrics)
- Load/save own profile
- Clear completion status indicator for the member

**Done when:** Member can complete and edit their profile on mobile and desktop.

---

## Spec 7 — Coach dashboard UI

- Roster table/list with completion flags
- Member detail view
- Metrics summary (total members, completed vs incomplete)
- Responsive layout

**Done when:** Coach sees all data, flags, and metrics.

---

## Spec 8 — Docs & verification

- Root README with run instructions
- CORS configured
- Smoke-test key API flows
- Commit, push, PR

**Done when:** App runs end-to-end from documented steps.
