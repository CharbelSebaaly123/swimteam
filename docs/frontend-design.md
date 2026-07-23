# Frontend Design (React)

**Audience:** Junior developers learning React SPAs  
**Companion:** [Architecture](./architecture.md) · [Backend Design](./backend-design.md)

---

## 1. What the frontend owns

The React app:

- Shows login / signup / profile / coach screens  
- Stores the JWT after login  
- Calls the Go API with `fetch`  
- Hides coach UI from members (UX only—server still enforces rules)

It does **not** decide security by itself.

---

## 2. Stack

| Piece | Role |
|-------|------|
| Vite | Dev server + build tool |
| React 19 | UI components |
| React Router | Routes (`/login`, `/profile`, `/coach`, …) |
| CSS | Responsive layout (no heavy UI kit required) |

Vite proxies `/api` → `http://localhost:8080` so the browser can call relative URLs without CORS pain in development.

---

## 3. App structure

```
src/
  main.jsx           # React root
  App.jsx            # routes
  auth.jsx           # AuthProvider + useAuth
  api.js             # fetch helpers with Bearer token
  imageUpload.js     # client-side resize before upload
  pages/             # one file per screen
  components/        # shell + ProtectedRoute
```

### Routes

| Path | Who | Screen |
|------|-----|--------|
| `/login` | Public | Login |
| `/signup` | Public | Member signup |
| `/change-password` | Auth | Current + new password |
| `/profile` | MEMBER | Edit personal profile + photo |
| `/coach` | COACH | Metrics, roster, age report |
| `/coach/profile` | COACH | Coach name/nickname/photo |
| `/coach/members/:userId` | COACH | Read-only member detail |

`ProtectedRoute` redirects wrong roles to their home page.

---

## 4. Auth context (simple mental model)

1. Login response includes `token` + user fields.  
2. We save that object in `localStorage`.  
3. `AuthProvider` exposes `{ user, login, logout, … }` to children.  
4. `api.js` reads the token and sets `Authorization: Bearer …`.

**Important:** `localStorage` is convenient for demos. XSS can steal tokens—keep dependencies updated and avoid `dangerouslySetInnerHTML`.

---

## 5. Calling REST from React

```js
// conceptual
await api('/api/profiles/me', { method: 'PUT', body: JSON.stringify(form) })
```

Good habits:

- Always handle non-OK responses and show `message` from the API.  
- Disable submit buttons while requests are in flight.  
- After password change, consider forcing re-login (or keep session if token still valid).

---

## 6. Responsive UI tips used here

- One primary job per page (profile form **or** coach roster).  
- Stack fields vertically on narrow screens.  
- Prefer full-width inputs on mobile.  
- Coach metrics are a simple summary strip—not a crowded dashboard of cards.

---

## 7. Photo upload UX

1. User picks an image.  
2. Browser canvas resizes (max side 800, JPEG ~0.75).  
3. Upload multipart `file` to `/api/profiles/me/photo`.  
4. Server resizes again (defense in depth) and stores JPEG bytes.

Client resize saves bandwidth; server resize protects you from huge/malicious uploads.

---

## 8. React best practices (junior checklist)

- [ ] Keep pages focused; extract repeated UI into components.  
- [ ] Derive UI from state (auth role → which nav links).  
- [ ] Don’t duplicate business rules that already live in Go (completion is server-driven).  
- [ ] Use controlled inputs for forms.  
- [ ] Clear errors when the user edits a field.  
- [ ] Prefer relative `/api/...` URLs with the Vite proxy in development.

---

## 9. How frontend + backend stay in sync

When the API adds a field:

1. Backend returns it in JSON  
2. Frontend form state adds the field  
3. Labels/help text explain validation (E.164 phone, mandatory DOB)  
4. Manual test on phone-width and desktop

That’s the whole loop.
