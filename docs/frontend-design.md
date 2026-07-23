# Frontend Design Document

**Audience:** Junior developers learning React  
**Companion docs:** [Architecture](./architecture.md) · [Backend Design](./backend-design.md)

This document explains how the SwimTeam **React frontend** is designed, and teaches practices you will reuse in most modern React apps:

1. Component structure & routing
2. Talking to a **REST** API from the browser
3. Handling **JWT** auth on the client
4. React best practices (state, effects, forms, responsive UI)

---

## 1. What the frontend is responsible for

The React app owns:

- Screens (login, signup, member profile, coach dashboard)
- Form UX (validation feedback, loading states, success/error messages)
- Navigation based on role
- Storing the JWT and attaching it to API calls
- Responsive layout for phones and desktops

It does **not** own security policy. The API can still reject unauthorized calls even if a buggy UI shows a button.

### Tech stack (frontend)

| Technology | Why we use it |
|------------|---------------|
| React 19 | UI components + hooks |
| Vite | Fast local dev server & build tool |
| React Router | Client-side routes (`/login`, `/coach`, …) |
| Plain CSS | Simple, teachable styling (no CSS framework required) |
| `fetch` | Browser HTTP client for REST |

Dev server: `http://localhost:5173`  
API (proxied): `/api` → `http://localhost:8080`

---

## 2. SPA concept (Single Page Application)

In a classic multi-page site, clicking a link downloads a new HTML document.

In an SPA:

1. The browser loads `index.html` once.
2. React mounts into `<div id="root">`.
3. React Router changes the URL and renders a different component tree.
4. Data comes from REST JSON calls, not from full page reloads.

```text
URL changes  →  Router picks a page component  →  Page fetches JSON  →  UI updates
```

This feels faster, but you must handle loading and error states yourself.

---

## 3. Folder structure

```
frontend/src/
├── main.jsx                 # React root + Router + AuthProvider
├── App.jsx                  # Route table
├── index.css                # Global responsive styles
├── api.js                   # REST client helpers
├── auth.jsx                 # Auth context (JWT + user)
├── components/
│   ├── AppShell.jsx         # Shared header/footer layout
│   └── ProtectedRoute.jsx   # Route guards by login/role
└── pages/
    ├── LoginPage.jsx
    ├── SignupPage.jsx
    ├── ProfilePage.jsx      # MEMBER only
    └── CoachDashboardPage.jsx  # COACH roster + detail
```

### Design rule for juniors

- **`pages/`** = screens matched to routes  
- **`components/`** = reusable pieces used by multiple pages  
- **`api.js`** = all HTTP calls in one place  
- **`auth.jsx`** = shared auth state

Avoid dumping every function into `App.jsx`.

---

## 4. Routing design

Defined in `App.jsx`:

| Path | Who | Screen |
|------|-----|--------|
| `/login` | Guests | Sign in |
| `/signup` | Guests | Member registration |
| `/profile` | MEMBER | Edit own profile |
| `/coach` | COACH | Metrics + roster |
| `/coach/members/:userId` | COACH | One member’s full profile |
| `/` | Anyone | Redirect by role |

### Protected routes

`ProtectedRoute` checks:

1. Is there a logged-in user? If not → `/login`.
2. If a `role` is required and it doesn’t match → redirect to that user’s home screen.

Example idea:

```jsx
<Route element={<ProtectedRoute role="COACH" />}>
  <Route path="/coach" element={<CoachDashboardPage />} />
</Route>
```

**Important:** this is UX protection, not real security. Real enforcement is on the Spring API.

---

## 5. REST from the browser

### 5.1 One API module

`api.js` wraps `fetch` so pages don’t repeat header logic:

```js
// Conceptual shape
api.login({ username, password })
api.getMyProfile(token)
api.updateMyProfile(token, payload)
api.getMembers(token)
api.getMetrics(token)
```

Benefits:

- Central place to add logging or change base URL
- Consistent JSON parsing
- Consistent error handling (`ApiError` with `status` + `message`)

### 5.2 Mapping UI actions to REST

| User action | HTTP call |
|-------------|-----------|
| Click **Sign in** | `POST /api/auth/login` |
| Click **Create account** | `POST /api/auth/signup` |
| Open profile page | `GET /api/profiles/me` |
| Click **Save profile** | `PUT /api/profiles/me` |
| Open coach dashboard | `GET /api/coach/metrics` + `GET /api/coach/members` |
| Click **View** on roster | `GET /api/coach/members/{userId}` |

### 5.3 Vite proxy (local development)

In `vite.config.js`, `/api` is proxied to the Spring server. That means frontend code can call:

```js
fetch('/api/auth/login', …)
```

…and the browser stays on the same origin (`localhost:5173`), which avoids many local CORS headaches.

In production you would typically:

- serve UI and API under one domain, or
- configure CORS carefully on the API

### 5.4 REST habits for frontend juniors

- Always handle non-OK responses (don’t assume `200`).
- Show the server `message` when present.
- Use loading flags so users know a request is in flight.
- Don’t put tokens in the URL.
- Prefer `PUT`/`POST` bodies as JSON with `Content-Type: application/json`.

---

## 6. JWT on the frontend

### 6.1 Where the token lives

After login/signup, the auth response includes `token` and `role`.  
`AuthProvider` stores the whole auth object in React state **and** `localStorage` so a refresh doesn’t log you out immediately.

```text
login success → save { token, username, role, ... }
             → later requests send Authorization: Bearer <token>
logout       → clear memory + localStorage
```

### 6.2 Auth context

`useAuth()` gives every page:

- `user`, `token`
- `isCoach`, `isMember`
- `login`, `signup`, `logout`

This is the **React Context** pattern: shared state without passing props through every layer (“prop drilling”).

### 6.3 Client JWT best practices

| Do | Don’t |
|----|-------|
| Send token only over trusted HTTP setup (HTTPS in prod) | Put JWT in query strings |
| Clear token on logout | Keep using a token after 401s without recovery |
| Gate routes for better UX | Trust the UI as security |
| Decode role only for display/routing | Trust client-modified role values |

If the API returns `401`, a good next step (future improvement) is to force logout and redirect to `/login`.

### 6.4 XSS awareness (short)

If malicious JavaScript runs in your page, it can read `localStorage` and steal the JWT.  
That is why many production apps prefer **httpOnly cookies** for tokens. For this learning app, localStorage keeps the flow easy to see and debug.

---

## 7. Page-by-page design

### Login & Signup

Goals:

- Simple forms
- Disable submit while request is running (`busy`)
- Show API errors with `role="alert"`
- Redirect by role after success

Signup is for **members only**. Coaches use the seeded `admin` account.

### Member profile (`ProfilePage`)

Goals:

- Load existing profile on mount (`useEffect`)
- Edit personal info + metrics in one form
- Show completion banner (`complete` / `incomplete`)
- Save with `PUT`, then refresh local form state from response

Required fields for completion (must match backend rules):

- first/last name, phone, date of birth
- emergency contact name/phone
- stroke specialty

### Coach dashboard

Goals:

- Fetch metrics + roster in parallel (`Promise.all`)
- Show completion flags clearly
- Link to detail view for each member
- Remain readable on small screens (table scrolls horizontally)

---

## 8. State management approach

This app uses **local component state + one auth context**.

That is enough when:

- Auth is global
- Each page’s form data is page-local
- You don’t yet need complex shared caches

### When juniors reach for heavier tools

You might later learn React Query / Redux Toolkit if:

- Many screens share the same server lists
- You need caching, retries, background refresh

Don’t add them until the pain appears. Complexity is a cost.

### Useful React hooks in this project

| Hook | Typical use here |
|------|------------------|
| `useState` | Form fields, errors, loading |
| `useEffect` | Load profile/roster when page opens |
| `useContext` (via `useAuth`) | Read JWT/user anywhere |
| `useNavigate` | Redirect after login |
| `useParams` | Read `:userId` on coach detail page |

### `useEffect` junior rules

1. Fetch data when the screen mounts.
2. Include dependencies that are actually used (`token`, `userId`).
3. Use a `cancelled` flag (or AbortController) so setState doesn’t run after unmount.
4. Don’t create infinite loops (avoid setting state that retriggers the same effect forever).

---

## 9. Forms and UX patterns

### Controlled inputs

React state is the source of truth:

```jsx
<input value={username} onChange={(e) => setUsername(e.target.value)} />
```

### Async submit pattern

Almost every form follows:

```text
preventDefault
→ clear old error
→ set busy true
→ await API
→ on success: navigate or show message
→ on failure: show err.message
→ finally: set busy false
```

Copy that pattern until it becomes muscle memory.

### Accessibility basics used here

- Labels wrapped around inputs (or clearly associated)
- `role="alert"` on errors
- Semantic landmarks (`header`, `main`, `nav`)
- Buttons disabled while saving to prevent double-posts

---

## 10. Responsive UI design

Styles live mainly in `index.css`.

Ideas used:

- Flexible layouts (`grid`, `clamp` font sizes)
- Auth screen stacks vertically on narrow viewports
- Metrics strip goes from 5 columns → 2 → compact grid
- Roster table sits in `overflow-x: auto` so phones can scroll sideways
- Sticky top bar for navigation

### Junior CSS habits

- Prefer relative units and flexible grids over fixed pixel pages
- Test at ~375px width and ~1280px width
- Keep one clear visual hierarchy: brand → page title → content
- Use motion sparingly (this app uses short fade/rise animations for polish)

---

## 11. React best practices checklist

### Components

- [ ] One component ≈ one job
- [ ] Pages orchestrate data; smaller components present UI
- [ ] Name files after what they render (`LoginPage`, not `Stuff`)

### Data & API

- [ ] Centralize HTTP in `api.js`
- [ ] Treat API as source of truth after saves
- [ ] Handle loading, success, and failure states explicitly

### Auth

- [ ] Keep token handling in one module/context
- [ ] Attach `Authorization` header for protected calls
- [ ] Redirect unauthenticated users to login

### Safety

- [ ] Never trust role checks in the UI alone
- [ ] Don’t render secrets into the DOM
- [ ] Sanitize assumptions about API data (`profile.phone || '—'`)

### Maintainability

- [ ] Avoid duplicating endpoint URLs across files
- [ ] Keep CSS variables for theme colors
- [ ] Delete unused scaffold files/styles when you outgrow them

### Performance (practical, not premature)

- [ ] Fetch on the pages that need the data
- [ ] Don’t store huge derived copies of the same list in five places
- [ ] Prefer simple code first; measure before optimizing

---

## 12. End-to-end picture (frontend view)

```text
main.jsx
  AuthProvider          ← owns JWT
    BrowserRouter
      App routes
        LoginPage  ──POST /api/auth/login──► API ──JWT──► AuthProvider
        ProfilePage ──GET/PUT /profiles/me ► API (Bearer token)
        Coach pages ──GET /coach/* ────────► API (Bearer + COACH role)
```

If you can draw this from memory, you understand the frontend architecture.

---

## 13. Suggested learning path through the code

1. `main.jsx` — how the app boots  
2. `App.jsx` — routes and guards  
3. `auth.jsx` — JWT storage and helpers  
4. `api.js` — REST calls  
5. `LoginPage.jsx` — simplest full flow  
6. `ProfilePage.jsx` — fetch + form + save  
7. `CoachDashboardPage.jsx` — parallel fetches + lists  
8. `index.css` — responsive rules at the bottom (`@media`)

Then modify something small (add a profile field label) and follow it from UI → `api.js` → backend DTO → database field.

---

## 14. Common junior pitfalls (and fixes)

| Pitfall | Fix |
|---------|-----|
| Fetching without handling errors | `try/catch` + show `err.message` |
| Forgetting Bearer token | Pass `token` into `api.*` helpers |
| Infinite `useEffect` loops | Fix dependency array; don’t set unused state every render |
| Only hiding coach links (no route guard) | Use `ProtectedRoute` **and** rely on API 403s |
| Giant components (500+ lines) | Split list/detail/form pieces |
| Copy-pasting `fetch` everywhere | Expand `api.js` instead |
| Ignoring mobile layout | Resize browser during development |

---

## 15. How frontend and backend stay aligned

Both sides must agree on:

- URL paths and HTTP methods
- JSON field names (`profileCompleted`, `personalBestSeconds`, …)
- Auth header format
- Error shape (`{ message }`)

When you add a feature, update in this order:

1. Backend contract (DTO + endpoint)
2. `api.js` helper
3. Page UI
4. Docs (if the contract changed)

That sequence prevents “UI finished but API doesn’t exist yet” surprises.
