# From local code to a pull request

Junior-friendly checklist for contributing to this repo. Base branch is **`main`**.

## 1. Update main

```bash
git checkout main
git pull origin main
```

## 2. Create a feature branch

```bash
git checkout -b cursor/short-description-of-change
```

Use a clear branch name. Keep one feature (or bugfix) per branch when you can.

## 3. Make your changes

Edit code, docs, or tests. Keep the change focused.

## 4. Verify locally

```bash
# Automated backend tests
cd backend && go test ./...

# Run the app
cd backend && go run ./cmd/server

# In another terminal
cd frontend && npm install && npm run dev
```

Open http://localhost:5173 and smoke-check the flow you touched.  
More detail: [testing.md](./testing.md).

## 5. Stage related files only

```bash
git status
git add path/to/file1 path/to/file2
git status   # double-check: no unrelated files
```

Do **not** commit build output (`backend/target/`, `frontend/dist/`, `node_modules/`).

## 6. Commit

```bash
git commit -m "Explain why this change exists in one short sentence"
```

## 7. Push your branch

```bash
git push -u origin HEAD
```

## 8. Open a pull request into `main`

**Option A — GitHub website**

1. Open the repo on GitHub.
2. Click **Compare & pull request** for your branch.
3. Set **base** = `main`, **compare** = your branch.
4. Fill title/description (what changed, how to test).
5. Create the PR.

**Option B — GitHub CLI**

```bash
gh pr create --base main --title "Your title" --body "What changed and how to test it"
```

## 9. After review

- Push more commits to the **same branch** to update the PR.
- When approved, use **Squash and merge** (or your team’s preferred merge style).
- Delete the feature branch after merge if your team does that.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| PR targets the wrong base | Base must be `main` |
| Closing the PR instead of merging | Use **Squash and merge**, not just Close |
| Committing secrets or huge binaries | Remove them; add to `.gitignore` |
| Forgetting to push | `git push -u origin HEAD` before opening the PR |
| Dirty working tree when asking for review | Commit or stash unrelated local files |
