# Project rules to seed

Write these three files under `.cursor/rules/` **verbatim** during the Seed step (before any `docs/` content). If a file already exists, reconcile—do not overwrite silently; note differences in the hand-off.

Push of a feature branch for an open PR is allowed when the user works via PRs; merge/close stays user-gated. That reconciles with `commit-after-steps` (“do not push unless asked”): ask or follow the user’s PR workflow before pushing.

---

## `.cursor/rules/commit-after-steps.mdc`

```mdc
---
description: Commit after completed work steps; never add Co-authored-by
alwaysApply: true
---

# Commits after steps

When a discrete work step is finished (docs batch, skill/rule, scaffold slice, feature slice) and the user asked to commit—or said to keep the branch tidy with commits—create a **focused git commit** for that step before moving on.

## Rules

- One step → one commit (split docs vs tooling vs app code when they landed together).
- Message: **English**, short, imperative, explains **why** (Conventional Commits).
- Stage only files that belong to that step.
- **Never** add `Co-authored-by` or other trailer crediting Cursor/the agent.
- Do not commit secrets, `node_modules`, build artifacts, or local DB files.
- Do not push unless the user asks (feature-branch push for an open PR: see `workflow-issue-pr-commit`).

## Message shape

| When | Format | Example |
|------|--------|---------|
| Issue known, PR not yet | `type(#ISSUE): subject` | `docs(#26): spec OpenFoodFacts hybrid import` |
| Issue and PR both known (later commits, squash/merge subject) | `type(#ISSUE): subject (#PR)` | `docs(#26): spec OpenFoodFacts hybrid import (#40)` |
| No issue (meta/chore on default branch) | `type: subject` | `chore: tighten commit message workflow rules` |

- Prefer GitHub autolink forms `#26` / `#40` (not bare `26` or `pr 40`).
- Do not invent a PR number before the PR exists; do not amend old commits solely to inject a PR id unless the user asks.
```

---

## `.cursor/rules/english-public-artifacts.mdc`

```mdc
---
description: All public repo artifacts must be English; Polish only in user chat and later UI strings
alwaysApply: true
---

# English for public artifacts

Everything **public** in this repository and on GitHub must be **English**.

## Must be English

- Documentation under `docs/` (ADRs, specs, runbooks, plans, vision, roadmap, open questions)
- Commit messages (Conventional Commits: `docs:`, `chore:`, `feat:`, …)
- GitHub issue and PR titles, bodies, and comments (where the agent can edit)
- Code identifiers, comments, and strings committed to the repo (except deferred app UI copy)

## Polish is OK only for

- Chat with the user in Cursor
- App UI strings later (UI/UX phase) — **do not invent i18n now**

## Agent obligations

- Do **not** write new Polish docs, ADRs, specs, issues, or PR text.
- When updating existing Polish content, translate it to clear professional English.
- Preserve structure: links, ADR numbers, Mermaid diagrams, OQ ids (`OQ-NN`).
- Prefer full English over leaving Polish quotes unless a short quoted user phrase is essential.
```

---

## `.cursor/rules/workflow-issue-pr-commit.mdc`

```mdc
---
description: After foundations, ship work as issue then PR then focused commits
alwaysApply: true
---

# Workflow: issue → PR → commit

After architecture foundations, implement work as **narrow vertical slices**, not large undocumented batches.

## Loop

1. **Issue** — capture the slice (use skill `to-issues` when breaking a plan/spec). Title and body in **English**. Acceptance criteria checkboxes are the agent’s done-list.
2. **Branch + PR** — open a PR early from a focused branch; describe intent in English (skill `pr`). Title uses the same shape as commits: `type(#ISSUE): subject`. Body links/closes the issue.
3. **Commits** — focused commits per step (`commit-after-steps`): `type(#ISSUE): subject`, then `type(#ISSUE): subject (#PR)` once the PR number exists.
4. **Acceptance criteria** — before asking to merge (or when AC are met): check off satisfied boxes on the issue via `gh` (only criteria actually done).
5. **Merge** — only when the user asks. Prefer squash subject `type(#ISSUE): subject (#PR)`. Do not merge/close issues or PRs unless the user explicitly requests it.
6. **After merge** — when the user asked for merge or cleanup, or merge is already on the default branch:
   - Delete the local feature branch and `git push origin --delete <branch>`.
   - Remove any worktree for that branch (`git worktree remove` / `prune`).
   - Do **not** use `git branch --merged` alone after squash merges — trust the merged PR as source of truth.
   - Leave `backup/*` and unrelated open implement issues alone.
   - If the user allows closing the issue: finish remaining AC checkboxes, then close.

## Push vs merge

- Pushing the **feature branch** so the PR stays up to date is OK when the user is working through PRs (ask if unclear).
- Do **not** push directly to the default branch, force-push shared history, or merge without an explicit user request.
```
