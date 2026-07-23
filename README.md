# swimteam

Responsive web app for swim team members to enter personal information, with coach admin access to all profiles, metrics, and completion status.

## Stack

- **Backend:** Java Spring Boot 3 (REST, JWT, Spring Security, JPA, H2)
- **Frontend:** React (Vite) + React Router

## Roles

| Role | Access |
|------|--------|
| `COACH` | Full roster, member details, team metrics, completion flags |
| `MEMBER` | Own profile only (after signup) |

**Default coach:** `admin` / `admin123`

## Specs

See [SPECS.md](./SPECS.md) for the step-by-step plan.

## Architecture & design docs (for junior developers)

| Document | What you’ll learn |
|----------|-------------------|
| [docs/architecture.md](./docs/architecture.md) | Big-picture system design, roles, request flows |
| [docs/backend-design.md](./docs/backend-design.md) | REST, JWT, Spring Boot layers & best practices |
| [docs/frontend-design.md](./docs/frontend-design.md) | React structure, auth context, calling APIs safely |

## Run locally

### 1. API (port 8080)

```bash
cd backend
mvn spring-boot:run
```

### 2. UI (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 — Vite proxies `/api` to the backend.

## API overview

| Method | Path | Who | Purpose |
|--------|------|-----|---------|
| POST | `/api/auth/signup` | Public | Member registration |
| POST | `/api/auth/login` | Public | Login → JWT |
| POST | `/api/auth/change-password` | Coach / Member | Change password (current + new) |
| GET/PUT | `/api/profiles/me` | Member | Own profile (email, E.164 phone, DOB required; address optional) |
| POST | `/api/profiles/me/photo` | Member | Upload photo (resized + JPEG compressed) |
| GET | `/api/profiles/me/photo` | Member | Download own photo |
| DELETE | `/api/profiles/me/photo` | Member | Remove photo |
| GET | `/api/coach/members?sort=&direction=` | Coach | Roster + completion flags (sort: `age`, `name`, `completed`, `username`) |
| GET | `/api/coach/members/{userId}` | Coach | Full member profile |
| GET | `/api/coach/members/{userId}/photo` | Coach | Member photo |
| GET | `/api/coach/metrics` | Coach | Aggregate metrics |
| GET | `/api/coach/reports/age-groups` | Coach | Members grouped by swim age brackets |

### Profile completion

A profile is marked `profileCompleted: true` when these are filled:

- First name, last name, email
- International phone (E.164, e.g. `+14155552671`)
- Date of birth (mandatory)
- Emergency contact name & international phone
- Stroke specialty

Address is optional.
## Project layout

```
backend/   Spring Boot REST service
frontend/  React SPA
SPECS.md   Spec-driven implementation plan
```
