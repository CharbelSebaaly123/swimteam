# Testing Guide (for junior developers)

**Audience:** Beginners who want to know *what* to test and *how* in this project.

Testing is how we gain confidence that login, profiles, and coach reports still work after we change code.

---

## 1. Types of tests (plain English)

| Type | What it checks | Speed | Example here |
|------|----------------|-------|--------------|
| **Unit** | One function in isolation | Fast | Age bracket helper, E.164 regex |
| **Integration / API** | HTTP routes + DB + auth together | Medium | `go test` with `httptest` |
| **Smoke** | “Can I start the app and log in?” | Manual / script | Login as admin |
| **Manual UI** | Buttons, forms, mobile layout | Slow | Click through React screens |

You do **not** need every type for every change. Start with API tests for security rules.

---

## 2. Automated backend tests

### Run them

```bash
cd backend
go test ./...
```

### What `TestAuthAccessAndCoachFlows` covers

1. Default coach `admin` / `admin123` can log in  
2. Sample member `sample` / `sample123` can log in  
3. Member can `GET /api/profiles/me`  
4. Coach calling member profile endpoint → **403**  
5. Member calling coach roster → **403**  
6. Coach can load roster, metrics, age-group report  
7. Signup + profile update + change-password works  

### What `TestPhotoUploadResizes` covers

Uploading a JPEG returns `hasPhoto: true` and `contentType: image/jpeg`.

These tests spin up a **temporary SQLite file** and seed data—no browser required.

---

## 3. Manual smoke checklist (API)

With the server running (`go run ./cmd/server`):

```bash
# health
curl -s http://localhost:8080/api/health

# login as coach
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'

# login as Maroun (sample member)
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"sample","password":"sample123"}'
```

Copy the `token` and call:

```bash
curl -s http://localhost:8080/api/coach/metrics \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 4. Manual UI checklist

1. Open http://localhost:5173  
2. Log in as **admin** → coach dashboard shows metrics + roster  
3. Sort by age; open age-group report  
4. Open coach profile—name Labib Waked, nickname Wahsh, photo visible  
5. Log out; log in as **sample** / **sample123**  
6. Profile shows Maroun Labib Waked, completed banner, photo  
7. Change a field; save; refresh—data persists  
8. Change password flow rejects wrong current password  
9. Narrow the browser (or phone) — forms still usable  

---

## 5. What “good enough” looks like before a PR

- [ ] `go test ./...` passes  
- [ ] Frontend `npm run build` succeeds  
- [ ] You personally logged in as coach **and** member once  
- [ ] README / SPECS still match reality  

---

## 6. Debugging tips

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| UI calls fail with network error | API not running | Start Go server on 8080 |
| 401 on every request | Missing/expired JWT | Log in again |
| 403 on coach pages as member | Expected | Use admin account |
| Port 8080 in use | Old process | Stop it, then restart |
| Empty roster | DB file wiped / wrong cwd | Restart server; check `swimteam.db` |

---

## 7. Junior habits that pay off

1. Test **security boundaries** first (member must not see other profiles).  
2. Reproduce bugs with a failing test when you can.  
3. Prefer a few reliable API tests over dozens of flaky UI tests.  
4. Write the expected status code in the test name or comments.  

When you change Go handlers, re-run `go test ./...` before you open a pull request.
