# SwimTeam Go — High-Level Architecture

**Audience:** Junior developers  
**Related docs:** [Backend Design](./backend-design.md) · [Frontend Design](./frontend-design.md) · [Testing](./testing.md)

---

## 1. What are we building?

A small web app for a swim team:

- **Members** sign up, log in, and fill out **their own** profile (contact info, metrics, photo).
- **Coaches** log in and see **everyone**: roster, completion flags, metrics, and age-group reports.

Two apps work together:

```
┌─────────────────────┐         HTTPS JSON + JWT         ┌─────────────────────┐
│   React Frontend    │  ──── Authorization: Bearer ───► │     Go REST API      │
│   (Vite, port 5173) │ ◄──── JSON / images ──────────── │   (Chi, port 8080)  │
└─────────────────────┘                                  └──────────┬──────────┘
                                                                    │
                                                                    ▼
                                                           ┌─────────────────┐
                                                           │  SQLite database│
                                                           │  (swimteam.db)  │
                                                           └─────────────────┘
```

The browser is **not** trusted. Security rules (who can see which profile) live on the **server**.

---

## 2. Client vs server (simple mental model)

| Piece | Runs where? | Job |
|-------|-------------|-----|
| React SPA | Browser | Forms, navigation, show data |
| Go API | Server | Auth, validation, database, photos |
| SQLite | Server disk | Persist users & profiles |

If you only hide a button in React, a curious user can still call the API. The Go service must reject forbidden requests with **401/403**.

---

## 3. Roles

| Role | How you get it | Access |
|------|----------------|--------|
| `MEMBER` | Sign up | Own profile + photo + password change |
| `COACH` | Seeded `admin` (or created offline) | All members, metrics, age reports, own coach profile |

Default coach: **`admin` / `admin123`** (Labib Waked, nickname Wahsh).

---

## 4. REST in one minute

**REST** means we model resources and use HTTP methods:

| Method | Meaning | Example |
|--------|---------|---------|
| `GET` | Read | `GET /api/profiles/me` |
| `POST` | Create / action | `POST /api/auth/login` |
| `PUT` | Replace/update | `PUT /api/profiles/me` |
| `DELETE` | Remove | `DELETE /api/profiles/me/photo` |

URLs name **nouns** (`/profiles`, `/members`), not verbs like `/getProfile`.  
Status codes tell the story: **200** OK, **201** created, **400** bad input, **401** not logged in, **403** logged in but not allowed, **404** missing, **409** conflict.

Data is usually **JSON**.

---

## 5. JWT in one minute

After login/signup the API returns a **JWT** (JSON Web Token): a signed string the browser stores and sends as:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

Inside the token (readable, but tamper-proof because of the signature):

- `sub` / username
- `uid` — user id
- `role` — `COACH` or `MEMBER`
- `exp` — expiry time

Middleware on every protected route:

1. Reads the header  
2. Verifies the signature with a secret  
3. Attaches claims to the request context  
4. Handlers check role / ownership

Never put secrets (passwords) inside a JWT. Never trust a JWT that fails signature checks.

---

## 6. Request flows

### Member updates profile

1. React `PUT /api/profiles/me` with JWT  
2. Go auth middleware validates JWT  
3. Service loads that user’s profile only  
4. Validates email, E.164 phone, DOB, etc.  
5. Saves SQLite row + recomputes `profileCompleted`  
6. Returns JSON profile

### Coach views roster sorted by age

1. React `GET /api/coach/members?sort=age&direction=asc`  
2. Middleware requires `COACH`  
3. Service loads all member profiles, sorts, returns summaries + completion flags

---

## 7. Project layout

```
backend/
  cmd/server/          # main() entrypoint
  internal/            # private app code (auth, handlers, services, models…)
  sample-data/         # seed photos
frontend/
  src/pages/           # Login, Signup, Profile, Coach dashboard…
docs/                  # You are here
SPECS.md               # Spec-driven plan
```

---

## 8. Why this architecture?

- **Separation:** UI can change without rewriting business rules.
- **Security at the edge of the API:** one place to enforce roles.
- **SQLite for learning:** one file, no install, great for local demos.
- **Spec-driven:** build and verify one vertical slice at a time (see `SPECS.md`).

Next: dive into [backend-design.md](./backend-design.md) for Go packages and JWT filters, then [frontend-design.md](./frontend-design.md) for React routing.
