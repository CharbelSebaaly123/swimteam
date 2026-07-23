# Backend Design Document

**Audience:** Junior developers learning Spring Boot APIs  
**Companion docs:** [Architecture](./architecture.md) · [Frontend Design](./frontend-design.md)

This document explains how the SwimTeam **backend** is designed, and teaches three pillars you will use in almost every professional Java API:

1. **REST**
2. **JWT authentication**
3. **Spring Boot best practices**

---

## 1. What the backend is responsible for

The Spring Boot app owns:

- Creating users (signup) and logging them in
- Hashing passwords (never store plain text)
- Issuing and validating JWTs
- Enforcing role rules (`MEMBER` vs `COACH`)
- Reading/writing profiles in the database
- Computing `profileCompleted` and coach metrics

It does **not** own buttons, CSS, or page navigation. That is the React app’s job.

### Tech stack (backend)

| Technology | Why we use it |
|------------|---------------|
| Spring Boot 3 | Fast way to build production-ready Java apps |
| Spring Web | REST controllers |
| Spring Security | Authentication & authorization |
| Spring Data JPA | Database access with repositories |
| H2 | Simple in-memory DB for local learning |
| JJWT | Create/parse JWT strings |
| Bean Validation | `@NotBlank`, `@Email`, etc. on request bodies |

Entry point: `com.swimteam.SwimTeamApplication`.

---

## 2. REST explained for juniors

### 2.1 Resources, not “pages”

In a website, URLs often mean **pages** (`/login.html`).  
In a REST API, URLs mean **resources** (data you can act on).

Examples in this project:

| Resource idea | URL |
|---------------|-----|
| Authentication actions | `/api/auth/signup`, `/api/auth/login` |
| The current member’s profile | `/api/profiles/me` |
| Coach’s member list | `/api/coach/members` |
| One member (coach view) | `/api/coach/members/{userId}` |
| Team metrics | `/api/coach/metrics` |

We prefix with `/api` so it is obvious these are machine endpoints, not HTML pages.

### 2.2 HTTP methods = verbs

```text
GET    → read something
POST   → create something / trigger an action (login, signup)
PUT    → update/replace a resource (save whole profile)
PATCH  → partial update (we didn’t need it yet)
DELETE → remove something
```

**Junior habit:** Don’t invent verbs in the URL like `/api/getProfile` or `/api/doLogin`. Prefer:

- `GET /api/profiles/me`
- `POST /api/auth/login`

### 2.3 Status codes you should memorize

| Code | Meaning | When we use it |
|------|---------|----------------|
| `200 OK` | Success | Login, get profile, metrics |
| `201 Created` | Something new was created | Signup |
| `400 Bad Request` | Validation failed | Missing required fields |
| `401 Unauthorized` | Not logged in / bad credentials | Wrong password |
| `403 Forbidden` | Logged in, but not allowed | Member hits coach endpoint |
| `404 Not Found` | Resource missing | Unknown member id |
| `409 Conflict` | State conflict | Username already taken |

### 2.4 JSON request / response

Example login request:

```json
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

Example response:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "admin",
  "email": "coach@swimteam.local",
  "role": "COACH"
}
```

REST APIs exchange **representations**, not HTML.

### 2.5 Statelessness

Each API call is independent. The server does not remember “this browser clicked login earlier” via a sticky server session for API auth. Instead, the client sends the JWT every time.

That makes scaling easier: any server instance can validate the token.

---

## 3. JWT explained for juniors

### 3.1 What JWT stands for

**JSON Web Token** — a compact string with three Base64url parts:

```text
header.payload.signature
```

Roughly:

1. **Header** — algorithm info (e.g. HS512)
2. **Payload** — claims (username, user id, role, expiry)
3. **Signature** — proof the server created it (HMAC with a secret key)

Anyone can *decode* the payload (it is not fully secret). Anyone **cannot** forge a valid signature without the secret.

So: treat JWT payload data as **readable but tamper-evident**.

### 3.2 Life of a token in this app

```
1. User posts username/password to /api/auth/login
2. Spring Security checks password hash
3. JwtTokenProvider builds a signed token with claims:
   - sub  = username
   - uid  = user id
   - role = COACH or MEMBER
   - exp  = expiry time
4. Frontend stores token (localStorage in this learning app)
5. Later requests include:
   Authorization: Bearer <token>
6. JwtAuthenticationFilter reads header, validates signature/expiry,
   loads the user, and sets SecurityContext
```

Key classes:

- `JwtTokenProvider` — create & parse tokens
- `JwtAuthenticationFilter` — runs once per request
- `SecurityConfig` — which URLs are public vs protected

### 3.3 Bearer scheme

`Bearer` means: “the client is presenting this token as proof.”

```http
Authorization: Bearer <jwt>
```

If the header is missing/invalid → typically `401`.  
If the token is valid but role is wrong → `403`.

### 3.4 JWT best practices (learn these early)

| Practice | Why |
|----------|-----|
| Use a long random secret | Short secrets are guessable |
| Set an expiration (`exp`) | Limits damage if stolen |
| Send over HTTPS in production | Stops network sniffing |
| Don’t put passwords in the token | Tokens are often readable |
| Validate signature on every request | Prevents tampering |
| Prefer httpOnly cookies for browser apps in production | Reduces XSS token theft risk |

> This learning project stores JWTs in `localStorage` for simplicity. In production apps, discuss cookie-based storage and XSS defenses with your team.

### 3.5 Authentication vs authorization (again)

- **Authentication:** “This JWT proves you are `swimmer1`.”
- **Authorization:** “`swimmer1` is a `MEMBER`, so `/api/coach/**` is denied.”

Spring Security handles both: filter authenticates; URL/`hasRole` rules authorize.

---

## 4. Package design (layered architecture)

```
com.swimteam
├── SwimTeamApplication      # bootstraps Spring
├── config                   # startup seeding (default admin)
├── domain                   # JPA entities + Role enum
├── dto                      # API request/response objects
├── repository               # Spring Data interfaces
├── security                 # JWT + SecurityFilterChain
├── service                  # business logic
└── web                      # REST controllers + error handler
```

### Why not put everything in controllers?

Because controllers would become huge and hard to test. Layers keep change local:

| Layer | Allowed to know about | Should not do |
|-------|----------------------|---------------|
| `web` | HTTP, DTOs, calling services | SQL, password hashing details |
| `service` | Domain rules, repositories | HTTP status details when avoidable |
| `repository` | Entities / queries | Role policy (“only coach…”) |
| `security` | Tokens, user details | Profile field validation |

---

## 5. Domain model

### 5.1 `User`

Represents a login identity:

- `username` (unique)
- `email` (unique)
- `passwordHash` (BCrypt, never plain text)
- `role` (`COACH` or `MEMBER`)

### 5.2 `MemberProfile`

One-to-one with a member `User`:

- Personal info: names, phone, DOB, address, emergency contacts
- Metrics: stroke specialty, personal best seconds, height, weight, notes
- `profileCompleted` boolean

`recomputeCompletion()` sets the flag when required fields are present. Coaches use that flag on the roster.

### 5.3 Why separate User and Profile?

- Not every user is a member with athlete data (coaches aren’t).
- Auth concerns (password, role) stay separate from profile concerns (PB time).
- Cleaner permissions: members update profile fields, not their own role.

---

## 6. Endpoint design

### Auth (`AuthController`)

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/auth/signup` | Public | Create MEMBER + empty profile, return JWT |
| POST | `/api/auth/login` | Public | Validate credentials, return JWT |

Signup always creates `Role.MEMBER`. Coaches are not self-registered in this design; `admin` is seeded.

### Member profiles (`ProfileController`)

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/profiles/me` | MEMBER | Read own profile |
| PUT | `/api/profiles/me` | MEMBER | Update own profile |

`/me` is intentional: the server uses the JWT identity, so a member cannot pass another user’s id and edit them.

### Coach (`CoachController`)

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/coach/members` | COACH | Roster + completion flags |
| GET | `/api/coach/members/{userId}` | COACH | Full profile |
| GET | `/api/coach/metrics` | COACH | Aggregates |

Protected in `SecurityConfig` with `.requestMatchers("/api/coach/**").hasRole("COACH")`.

---

## 7. Spring Security design

### 7.1 SecurityFilterChain (the traffic cop)

In `SecurityConfig`:

1. Disable CSRF for this stateless JWT API (CSRF mainly matters for cookie session forms).
2. Enable CORS for the React origin.
3. Set session policy to **STATELESS**.
4. Permit `/api/auth/**`.
5. Restrict `/api/coach/**` to `ROLE_COACH`.
6. Require authentication for everything else relevant.
7. Insert `JwtAuthenticationFilter` before username/password filter.

### 7.2 Password hashing

We use `BCryptPasswordEncoder`.

- On signup/seed: encode password → store hash.
- On login: Spring matches raw password against hash.

**Never log passwords. Never return password hashes in API responses.**

### 7.3 Default coach seed

`DataSeeder` runs at startup (`CommandLineRunner`):

- If `admin` does not exist, create a `COACH` with password from config (`admin123` in `application.yml`).

This is great for demos; production would use secrets management and force password rotation.

---

## 8. DTOs and validation

We do **not** expose JPA entities directly as JSON. We use DTOs:

- `SignupRequest`, `LoginRequest`
- `ProfileUpdateRequest`, `ProfileResponse`
- `MemberSummaryResponse`, `CoachMetricsResponse`

### Why DTOs?

1. Hide internal fields (`passwordHash`).
2. Shape responses for UI needs (summary vs full detail).
3. Validate input with annotations:

```java
@NotBlank
@Size(min = 3, max = 64)
private String username;
```

Invalid bodies become `400` via `GlobalExceptionHandler`.

### Junior tip

If you add a field to the database entity, decide deliberately whether it belongs in the request DTO, response DTO, both, or neither.

---

## 9. Error handling

`GlobalExceptionHandler` centralizes errors into a simple `{ "message": "..." }` JSON body.

That keeps controllers clean and gives the React app a consistent error shape.

---

## 10. Spring best practices checklist

Use this as a study checklist while reading the code:

### Structure & design

- [ ] Keep controllers thin; put rules in services
- [ ] Use constructor injection (what this project does) instead of field `@Autowired`
- [ ] Separate domain entities from API DTOs
- [ ] Name packages by responsibility (`web`, `service`, `security`)

### REST

- [ ] Use nouns for resources, HTTP methods for actions
- [ ] Return meaningful status codes
- [ ] Keep URLs consistent (`/api/coach/members/{id}`)
- [ ] Avoid breaking changes casually (frontend depends on JSON shapes)

### Security

- [ ] Hash passwords (BCrypt)
- [ ] Authenticate with JWT (or sessions — pick one model and stick to it)
- [ ] Authorize on the server for every sensitive endpoint
- [ ] Least privilege: members cannot hit coach routes
- [ ] Externalize secrets via config (and use real secrets in production)

### Data

- [ ] Use `@Transactional` on service methods that write data
- [ ] Recompute derived fields in one place (`recomputeCompletion`)
- [ ] Validate inputs before saving

### Operability

- [ ] Clear application config (`application.yml`)
- [ ] Sensible logging on startup (admin seeded)
- [ ] Don’t commit build output (`backend/target/`)

---

## 11. Configuration map

From `application.yml`:

| Key | Purpose |
|-----|---------|
| `server.port` | API port (`8080`) |
| `spring.datasource.*` | H2 connection |
| `app.jwt.secret` | HMAC signing key |
| `app.jwt.expiration-ms` | Token lifetime |
| `app.cors.allowed-origins` | React origins allowed |
| `app.admin.*` | Default coach credentials |

---

## 12. Suggested learning path through the code

Read in this order:

1. `Role.java`, `User.java`, `MemberProfile.java` — data model  
2. `AuthController` + `AuthService` — signup/login flow  
3. `JwtTokenProvider` + `JwtAuthenticationFilter` — how identity moves  
4. `SecurityConfig` — who can call what  
5. `ProfileService` + `ProfileController` — member rules  
6. `CoachController` — admin reads  
7. `GlobalExceptionHandler` — failure paths  

Then open the Network tab in the browser and watch each call match what you read.

---

## 13. Common junior pitfalls (and fixes)

| Pitfall | Better approach |
|---------|-----------------|
| Returning entities with password hashes | Use response DTOs |
| Checking role only in React | Enforce in Spring Security + services |
| Using `GET` for login | Use `POST` (credentials in body) |
| Giant “Utils” class for everything | Prefer focused services |
| Catching exceptions and returning `200` with error text | Use proper HTTP status codes |
| Hardcoding secrets in source for production | Environment variables / secret stores |

---

## 14. How this backend supports the frontend

The React app expects:

- Auth responses with `token` + `role`
- Profile JSON including `profileCompleted`
- Coach roster items with completion flags
- Metrics object with counts and averages
- Error bodies with `message`

Stable contracts between backend and frontend are part of good API design. When you change a field name, update both sides (or version your API).
