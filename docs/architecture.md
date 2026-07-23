# High-Level Architecture

**Audience:** Junior developers learning how a full-stack web app fits together  
**Project:** SwimTeam Profiles — members manage their own data; coaches see everything

This document explains the big picture. For deeper detail, see:

- [Backend Design](./backend-design.md) — Spring Boot, REST, JWT
- [Frontend Design](./frontend-design.md) — React, routing, calling APIs
- [Testing Guide](./testing.md) — unit, integration, smoke, and manual checklists

---

## 1. What problem does this app solve?

A swim team needs:

1. **Members** to create an account and fill in personal info + swim metrics.
2. **Coaches** to see every member, who finished their profile, and team-wide stats.

Those two roles must not see the same things. Architecture is how we draw those boundaries clearly.

---

## 2. Mental model: two apps talking over HTTP

```
┌─────────────────────┐         HTTP + JSON          ┌─────────────────────┐
│                     │  ─────────────────────────►  │                     │
│   React Frontend    │     Authorization: Bearer    │  Spring Boot API    │
│   (browser)         │  ◄─────────────────────────  │  (server)           │
│                     │         JWT + data           │                     │
└─────────────────────┘                              └──────────┬──────────┘
                                                                │
                                                                ▼
                                                     ┌─────────────────────┐
                                                     │  H2 Database        │
                                                     │  (users, profiles)  │
                                                     └─────────────────────┘
```

| Piece | Runs where? | Job |
|-------|-------------|-----|
| **Frontend** | User’s browser | Screens, forms, navigation |
| **Backend API** | Server (your machine in local dev) | Rules, security, business logic |
| **Database** | Server process (H2 in-memory here) | Durable storage of users & profiles |

**Why split them?**  
The browser is not trusted. Anyone can open DevTools and change JavaScript. Real security and data rules live on the server.

---

## 3. Architecture style: client–server + REST API

This project uses a **client–server** style with a **REST API** in the middle:

- The React app is the **client**.
- The Spring Boot app is the **server**.
- They communicate with **REST** endpoints (URLs that accept HTTP methods like `GET` / `POST` / `PUT`).

### What is REST? (short version)

**REST** (Representational State Transfer) is a way to design web APIs so that:

- Each **resource** has a URL (example: `/api/profiles/me`).
- You use **HTTP methods** to say what you want to do:

| Method | Meaning | Example in this app |
|--------|---------|---------------------|
| `GET` | Read | Get my profile |
| `POST` | Create / start an action | Sign up, log in |
| `PUT` | Replace / update | Save my full profile |
| `DELETE` | Remove | (not used yet) |

- Data is usually sent as **JSON** (JavaScript Object Notation).
- The API should be **stateless**: each request carries enough info (here: a JWT) so the server does not need a server-side “login session” for every click.

You will see REST explained more in the [backend design doc](./backend-design.md).

---

## 4. Security at a glance: JWT

After login or signup, the backend returns a **JWT** (JSON Web Token). The frontend stores it and sends it on later requests:

```http
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

Think of the JWT as a **signed ID badge**:

- The server created it and signed it with a secret.
- The client cannot forge a valid badge without that secret.
- The badge says who you are (`admin`) and what role you have (`COACH` or `MEMBER`).

**Important:** JWTs are not magic encryption of your whole account. They are a portable proof of authentication. Keep them out of public places (URL query strings, screenshots, chat logs).

Details: [Backend Design → JWT](./backend-design.md#3-jwt-explained-for-juniors).

---

## 5. Roles and access (authorization)

Authentication = “Who are you?”  
Authorization = “What are you allowed to do?”

| Role | How you get it | What you can do |
|------|----------------|-----------------|
| `MEMBER` | Sign up | Read/update **only your** profile |
| `COACH` | Seeded default user `admin` | List all members, view any profile, see metrics |

The frontend **hides** coach screens from members for UX, but the backend **enforces** the rules. Always enforce on the server.

```
Member token ──► /api/profiles/me     ✅
Member token ──► /api/coach/members   ❌ 403 Forbidden

Coach token  ──► /api/coach/members   ✅
Coach token  ──► /api/profiles/me     ❌ (coaches don’t have member profiles)
```

---

## 6. Main request flows

### 6.1 Member signup → complete profile

```
Browser                API                     Database
   │                    │                         │
   │ POST /api/auth/signup                        │
   │───────────────────►│ create User+Profile     │
   │                    │────────────────────────►│
   │◄── JWT + role MEMBER                         │
   │                    │                         │
   │ PUT /api/profiles/me  (+ Bearer JWT)         │
   │───────────────────►│ validate & save         │
   │                    │ set profileCompleted    │
   │◄── updated profile JSON                      │
```

### 6.2 Coach views roster and metrics

```
Browser                API
   │ POST /api/auth/login (admin)
   │───────────────────►│
   │◄── JWT + role COACH
   │
   │ GET /api/coach/metrics
   │ GET /api/coach/members
   │───────────────────►│ (role must be COACH)
   │◄── counts + roster with completion flags
```

---

## 7. Project layout (monorepo)

```
swimteam/
├── backend/          # Spring Boot REST API (port 8080)
├── frontend/         # React + Vite SPA (port 5173)
├── docs/             # Architecture & design docs (you are here)
├── SPECS.md          # Spec-driven build plan
└── README.md         # How to run the app
```

In local development, Vite **proxies** `/api` to `http://localhost:8080`, so the browser can call `/api/...` without worrying about CORS during day-to-day coding.

---

## 8. Layered backend (why packages matter)

Inside `backend/`, code is split by responsibility:

```
web (controllers)     → HTTP in/out only
service               → business rules
repository            → database access
domain                → entities (User, MemberProfile)
security              → JWT + Spring Security
dto                   → request/response shapes
```

This is sometimes called **layered architecture**. Juniors should learn:

- Controllers should stay thin.
- Services own the rules (e.g. “recompute profileCompleted”).
- Repositories should not contain business policy.

---

## 9. Frontend structure (SPA)

The React app is a **Single Page Application (SPA)**:

- The browser loads one HTML page.
- React Router swaps screens without full page reloads.
- Pages call the API with `fetch`.

Key ideas:

- **Auth context** holds the logged-in user + JWT.
- **Protected routes** send guests to login and block wrong roles.
- **Pages** own screen-specific state (forms, loading, errors).

---

## 10. Data the system cares about

```
User
 ├── id, username, email, passwordHash, role
 └── optional MemberProfile (members only)
        ├── personal fields (name, phone, DOB, …)
        ├── metrics (stroke, PB seconds, height, weight)
        └── profileCompleted (true/false flag for coaches)
```

Completion is not a separate table. It is a **derived flag** recomputed when a member saves their profile.

---

## 11. Non-goals (what this architecture deliberately skips)

For learning clarity, this project does **not** yet include:

- Refresh tokens / logout blacklists
- Password reset email flows
- Production database (Postgres, etc.)
- File uploads (profile photos)
- Multi-team / multi-tenant orgs

Those are natural next steps once the basics feel solid.

---

## 12. How to read the rest of the docs

| If you want to learn… | Read |
|-----------------------|------|
| REST verbs, status codes, Spring layers, JWT filters | [backend-design.md](./backend-design.md) |
| Components, hooks, routing, calling APIs safely | [frontend-design.md](./frontend-design.md) |
| What was built step by step | [../SPECS.md](../SPECS.md) |

### Junior tip

When something breaks, ask:

1. Did the **request** leave the browser? (Network tab)
2. Did the **API** accept it? (status code 200/201 vs 401/403/400)
3. Did the **JWT** get sent?
4. Is this a **UI** bug or a **server rule**?

That habit will save you hours.
