# Backend Design (Go REST API)

**Audience:** Junior developers learning Go APIs  
**Companion:** [Architecture](./architecture.md) · [Frontend Design](./frontend-design.md)

This document explains how the SwimTeam **backend** is designed, and teaches three pillars you will use in almost every professional API:

1. **REST**
2. **JWT authentication**
3. **Go best practices**

---

## 1. What the backend owns

The Go app owns:

- User accounts and password hashes  
- Profile data and completion flags  
- Photo resize/compress + storage  
- Coach metrics and age-group reports  
- Authorization (member vs coach)

The React app only **displays** data and collects input.

---

## 2. Tech choices (and why)

| Library | Why juniors like it |
|---------|---------------------|
| **Chi** | Lightweight HTTP router, middleware-friendly |
| **GORM** | Simple ORM for SQLite/Postgres-style models |
| **SQLite** | One file DB—no Docker required for local demos |
| **golang-jwt** | Standard way to create/verify JWTs |
| **bcrypt** | Industry default for hashing passwords |
| **imaging** | Resize pictures before saving |

---

## 3. REST design for this project

### Resources

| Resource | Base path |
|----------|-----------|
| Auth actions | `/api/auth` |
| Current member profile | `/api/profiles/me` |
| Coach tools | `/api/coach/...` |

### Good habits

- Prefer nouns in paths (`/members`, not `/getAllMembers`).  
- Use correct verbs (`PUT` to update profile, `POST` for login).  
- Return consistent JSON errors: `{ "message": "..." }`.  
- Use status codes honestly (don’t return 200 for failures).

### Example: login

```http
POST /api/auth/login
Content-Type: application/json

{"username":"sample","password":"sample123"}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"token":"...","tokenType":"Bearer","role":"MEMBER", ...}
```

---

## 4. JWT step-by-step

### Create (login/signup)

1. Verify username + password (bcrypt compare).  
2. Build claims: `uid`, `role`, `username`, `exp`.  
3. Sign with HMAC-SHA256 and a server secret.  
4. Return the string to the client.

### Verify (every protected request)

1. Read `Authorization: Bearer <token>`.  
2. Parse + verify signature + expiry.  
3. Put claims in `context.Context`.  
4. Handlers read `UserID` / `Role` from context.

### Authorization vs authentication

- **Authentication:** “Who are you?” → valid JWT  
- **Authorization:** “What may you do?” → role checks + ownership

Example: a member JWT is valid, but `GET /api/coach/members` still returns **403**.

---

## 5. Package layout (Go best practice)

```
cmd/server/main.go     # wires config, DB, router; starts HTTP
internal/config        # env defaults
internal/models        # User, MemberProfile
internal/db            # GORM open + AutoMigrate
internal/auth          # bcrypt + JWT
internal/middleware    # Authenticate, RequireRole
internal/services      # business rules
internal/handlers      # HTTP adapters (JSON in/out)
internal/photo         # resize + JPEG encode
internal/seed          # admin + sample member
```

**Rule of thumb:** handlers talk HTTP; services talk business; models talk data.

Keeping code under `internal/` means other Go modules cannot import it by accident—good encapsulation.

---

## 6. Domain rules worth remembering

### Profile completion

`profileCompleted` is **computed on the server** when a profile is saved. Required for `true`:

- first/last name  
- E.164 phone (`+` and country code)  
- date of birth  
- emergency contact name + E.164 phone  
- stroke specialty  

Address is optional. Photo is optional.

### Photos

Upload pipeline:

1. Accept jpeg/png/gif/webp  
2. Decode image  
3. Fit so longest side ≤ 800px  
4. Encode JPEG ~quality 0.75  
5. Store bytes in SQLite (BLOB)

This keeps demos simple (no S3). Production apps often use object storage instead.

### Age groups

Brackets: `8 & under`, `9–10`, `11–12`, `13–14`, `15–16`, `17–18`, `19+`.  
Members without DOB go to `membersWithUnknownAge`.

---

## 7. Security checklist (junior → solid)

- [ ] Hash passwords with bcrypt (never store plaintext).  
- [ ] Keep JWT secret long and out of git in real deployments.  
- [ ] Enforce roles on the **server**.  
- [ ] Validate phone/email/DOB before saving.  
- [ ] Limit upload size; re-encode images server-side.  
- [ ] Use HTTPS in production so Bearer tokens are not stolen on the wire.

---

## 8. Go best practices used here

1. **Small interfaces / clear packages** — each folder has one job.  
2. **Explicit errors** — sentinel errors (`ErrNotFound`) mapped to HTTP status.  
3. **Context for request-scoped values** — JWT claims travel via `context`.  
4. **Middleware chains** — auth and role checks wrap route groups.  
5. **Config via env with safe defaults** — easy local runs, overridable in deploy.  
6. **Table-driven tests / HTTP tests** — `httptest` hits real routes without a browser.

---

## 9. How to extend this API

Want a new field like `clubName`?

1. Add column on `MemberProfile`  
2. AutoMigrate picks it up  
3. Extend DTO + validation in service  
4. Update React form  
5. Add a test asserting the field round-trips  

That vertical slice is the same pattern every time.

---

## 10. Common junior mistakes

| Mistake | Fix |
|---------|-----|
| Checking role only in React | Also enforce in Go middleware/services |
| Returning stack traces to clients | Log server-side; return `{message}` |
| Using GET for password change | Use `POST /api/auth/change-password` |
| Storing JWT secret as `"secret"` in prod | Use a long random env var |

When you finish this file, skim [frontend-design.md](./frontend-design.md) to see how React consumes these endpoints.
