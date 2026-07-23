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
| GET/PUT | `/api/profiles/me` | Member | Own profile |
| GET | `/api/coach/members` | Coach | Roster + completion flags |
| GET | `/api/coach/members/{userId}` | Coach | Full member profile |
| GET | `/api/coach/metrics` | Coach | Aggregate metrics |

### Profile completion

A profile is marked `profileCompleted: true` when these are filled:

- First name, last name, phone, date of birth
- Emergency contact name & phone
- Stroke specialty

## Project layout

```
backend/   Spring Boot REST service
frontend/  React SPA
SPECS.md   Spec-driven implementation plan
```
