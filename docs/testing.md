# Testing Guide (for Junior Developers)

**Audience:** Juniors learning how we verify the SwimTeam app works  
**Companion docs:** [Architecture](./architecture.md) · [Backend Design](./backend-design.md) · [Frontend Design](./frontend-design.md)

This guide explains:

1. What kinds of testing exist (and what each is for)
2. What this project already includes
3. How to run automated tests
4. A manual checklist you can follow in the browser
5. How to add your first new tests

---

## 1. Why we test (short version)

Testing answers: **“Did we break something?”** and **“Does the feature do what we promised?”**

Without tests, every change is a guessing game. With tests, you get faster feedback and more confidence before you open a pull request.

---

## 2. Types of tests (learn these names)

Think of testing like a pyramid:

```text
        /\
       /  \        End-to-end (few)     — full UI + API together
      /----\
     /      \      Integration         — API + DB + security together
    /--------\
   /          \    Unit                — one class/function in isolation
  /------------\
```

| Type | What it checks | Speed | Example in this app |
|------|----------------|-------|---------------------|
| **Unit** | One small piece of logic | Very fast | Phone number format, image resize math |
| **Integration** | Several layers together (controller → service → DB) | Medium | Login returns JWT; member cannot call coach API |
| **Smoke** | “Does the app basically start and respond?” | Fast/manual | Login as admin, open roster |
| **Manual / exploratory** | Real UI clicks, visual checks | Slow | Upload a photo, resize browser to mobile |
| **End-to-end (E2E)** | Browser automation through full flows | Slowest | (Not set up yet — future improvement) |

Juniors often over-focus on E2E. Start with **unit + API integration** tests; use **manual checklists** for UI polish.

---

## 3. What this project includes today

### 3.1 Automated backend tests (JUnit + Spring Boot Test)

Location: `backend/src/test/java/...`

Dependencies already in `backend/pom.xml`:

- `spring-boot-starter-test` (JUnit 5, MockMvc, AssertJ, Mockito)
- `spring-security-test` (helpers for authenticated requests)

Included tests:

| Test class | What it covers |
|------------|----------------|
| `PhoneNumbersTest` | E.164 phone validation rules |
| `ImageProcessingServiceTest` | Upload is resized and converted to JPEG |
| `AuthAndAccessIntegrationTest` | Signup/login, JWT access, member vs coach permissions, nickname/profile basics |

### 3.2 Manual / smoke testing

During feature work we repeatedly verified flows with HTTP calls and the UI:

- Coach login (`admin` / `admin123`)
- Sample member login (`sample` / `sample123`)
- Profile validation (bad phone rejected, good phone accepted)
- Photo upload compression
- Coach age sorting and age-group report
- Change password

Those checks are captured as a **manual checklist** later in this doc so you can repeat them.

### 3.3 What is not included yet

- Automated React component tests (Vitest/React Testing Library)
- Browser E2E framework (Playwright/Cypress)
- Performance / load testing
- Accessibility audit automation

That is normal for an early learning project. Add them when the team needs them.

---

## 4. How to run automated tests

### Backend (from repo root)

```bash
cd backend
mvn test
```

Useful variants:

```bash
# One class
mvn -Dtest=PhoneNumbersTest test

# One method
mvn -Dtest=AuthAndAccessIntegrationTest#memberCannotAccessCoachEndpoints test

# Skip tests during packaging (only when you intentionally want a fast build)
mvn -DskipTests package
```

**What “green” looks like:** Maven prints `BUILD SUCCESS` and each test method passes.

### Frontend

There is no automated frontend test runner configured yet. For now:

```bash
cd frontend
npm run build    # catches many syntax/import errors
npm run lint     # if oxlint reports issues
npm run dev      # then use the manual UI checklist
```

---

## 5. How the backend tests are structured (read this first)

### Unit test example mindset

`PhoneNumbersTest` does **not** start the web server. It only checks a helper:

```text
input "+14155552671" → valid
input "555-0100"     → invalid
```

This is the cheapest kind of test. Prefer unit tests for pure rules (validation, formatting, calculations).

### Integration test example mindset

`AuthAndAccessIntegrationTest` starts a Spring context with H2 and calls real HTTP endpoints using **MockMvc** (no browser needed).

Typical pattern:

1. `POST /api/auth/signup` or login
2. Read JWT from JSON response
3. Call a protected endpoint with `Authorization: Bearer …`
4. Assert status code (`200`, `403`, `400`) and JSON fields

### Junior tip: assert behavior, not implementation

Prefer:

- “Member gets `403` on `/api/coach/members`”
- “Profile with local phone is rejected”

Avoid:

- “Service method X called Y exactly once” (brittle, hard to maintain)

---

## 6. Manual test checklist (UI + API)

Run both servers:

```bash
# Terminal 1
cd backend && mvn spring-boot:run

# Terminal 2
cd frontend && npm install && npm run dev
```

Open http://localhost:5173

### A. Auth & roles

| # | Steps | Expected |
|---|-------|----------|
| A1 | Login `admin` / `admin123` | Lands on coach dashboard; footer shows Labib Waked (“Wahsh”) |
| A2 | Open **My Profile** (coach) | Photo visible; name Labib / Waked; nickname Wahsh |
| A3 | Logout, login `sample` / `sample123` | Lands on member profile; Maroun Labib Waked with photo |
| A4 | As member, try opening `/coach` in the URL bar | Redirected away (member cannot use coach pages) |
| A5 | Sign up a new user | Account created; empty/incomplete profile |

### B. Change password

| # | Steps | Expected |
|---|-------|----------|
| B1 | Login as member → **Password** | Form asks for current + new + confirm |
| B2 | Wrong current password | Error message |
| B3 | Correct current + new password | Success; can login with new password |

### C. Member profile rules

| # | Steps | Expected |
|---|-------|----------|
| C1 | Phone `555-0100` | Rejected (must be E.164 like `+14155550100`) |
| C2 | Missing date of birth | Cannot complete / validation error |
| C3 | Fill required fields + emergency E.164 phone | Banner becomes **Profile complete** |
| C4 | Leave address empty | Still allowed (optional) |
| C5 | Set nickname | Saved; coach roster can show nickname |

### D. Photos

| # | Steps | Expected |
|---|-------|----------|
| D1 | Upload a large PNG/JPEG on member profile | Preview appears; success message shows compressed size |
| D2 | Coach opens that member’s detail | Photo visible |
| D3 | Coach uploads/replaces own photo on coach profile | Preview updates |

### E. Coach features

| # | Steps | Expected |
|---|-------|----------|
| E1 | Dashboard metrics | Counts match number of members / completed profiles |
| E2 | Sort roster by **Age** | Order changes by age |
| E3 | Age group report | Members appear in the correct bracket |
| E4 | View member detail | Full profile + completion flag + photo |

### F. API-only smoke (optional, with curl)

```bash
# Login coach
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'

# Copy token, then:
curl -s http://localhost:8080/api/coach/metrics \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

Expect JSON with `totalMembers`, `completedProfiles`, etc.

---

## 7. Seeded accounts used for testing

| Username | Password | Role | Notes |
|----------|----------|------|-------|
| `admin` | `admin123` | COACH | Labib Waked (“Wahsh”) + photo |
| `sample` | `sample123` | MEMBER | Maroun Labib Waked + completed profile + photo |

H2 is in-memory: **restarting the API resets the database** and re-runs the seeder.

---

## 8. Adding your first new test (recipe)

### Backend unit test

1. Create `backend/src/test/java/com/swimteam/.../YourTest.java`
2. Annotate with `@Test` methods (JUnit 5)
3. Run `mvn -Dtest=YourTest test`

### Backend API integration test

1. Use `@SpringBootTest` + `@AutoConfigureMockMvc`
2. Inject `MockMvc`
3. Perform requests and assert status/JSON
4. Keep tests independent (don’t rely on leftover data from another class if avoidable)

### Frontend test (future)

When you add Vitest:

1. Test pure helpers first (`imageUpload.js`, phone regex)
2. Then test components with React Testing Library (buttons, form errors)
3. Keep network calls mocked

---

## 9. Good junior testing habits

| Do | Don’t |
|----|-------|
| Name tests after behavior (`rejectsLocalPhoneNumber`) | Name tests `test1`, `test2` |
| Check both success and failure paths | Only test the happy path |
| Test security boundaries (member vs coach) | Assume the UI hide = security |
| Keep fixtures small and readable | Copy huge JSON blobs everywhere |
| Run `mvn test` before pushing | Push and hope CI (or your teammate) notices |
| Update this checklist when features change | Leave docs describing old behavior |

---

## 10. Mapping features → what to test

| Feature | Automated ideas | Manual ideas |
|---------|-----------------|--------------|
| Signup/login/JWT | Integration: status + token + role | Login both roles in UI |
| Change password | Integration: wrong current password → 400 | Change then re-login |
| Profile validation | Unit: phone helper; Integration: 400 on bad phone | Try invalid phone in form |
| Profile completion flag | Integration: incomplete vs complete JSON | Watch banner switch |
| Photo upload | Unit/service: output is JPEG and smaller/resized | Upload big image, confirm preview |
| Coach roster/sort | Integration: sort=age order | Toggle sort controls |
| Age group report | Integration: member appears in expected bucket | Check report cards |
| Nickname | Integration: saved on profile/coach me | Visible in footer/roster |

---

## 11. Troubleshooting failed tests

| Symptom | Likely cause | What to try |
|---------|--------------|-------------|
| Port 8080 already in use during manual test | Old API still running | Stop the old Java process and restart |
| Integration test fails on seeded users | Test context conflict / dirty DB assumptions | Prefer creating unique usernames per test |
| Photo test fails in CI/Linux | AWT/headless issue | Ensure `-Djava.awt.headless=true` (Spring tests usually set this) |
| Frontend “works on my machine” but API 401 | Missing/expired JWT | Login again; check `Authorization` header |
| `403` when you expected `200` | Wrong role token | Confirm you logged in as coach vs member |

---

## 12. Quick command cheat sheet

```bash
# Automated backend suite
cd backend && mvn test

# Start app for manual testing
cd backend && mvn spring-boot:run
cd frontend && npm run dev

# Production-like frontend compile check
cd frontend && npm run build
```

---

## 13. Learning path

1. Run `mvn test` and read one failing assertion message on purpose (break a test locally, then fix it).
2. Read `PhoneNumbersTest` (simplest).
3. Read `AuthAndAccessIntegrationTest` (most realistic).
4. Add one new assertion for a bug you find.
5. Only then consider UI automation tools.

If you can explain **why** a member gets `403` on a coach endpoint, you already understand the most important test in this project.
