# Git workflow

## Branches

| Branch | Role |
|--------|------|
| `master` (or later `main`) | Stable line; protected when CI exists |
| `feat/…`, `fix/…`, `docs/…`, `chore/…` | Short-lived work branches |

At start (solo): short branches + PR to `master` even with one author — history and docs review stay readable.

## Commits

- Imperative mood, **English** Conventional Commits (see `english-public-artifacts` Cursor rule).

Examples:

```
docs(#26): spec OpenFoodFacts hybrid import
docs(#26): spec OpenFoodFacts hybrid import (#40)
feat(core): add FoodEntry validation
fix(auth): refresh session before sync
```

- One intent per commit.
- Do not commit `node_modules`, builds, local `.env`, or databases with personal data.

## Pull requests

- Description: **why** + how to test.
- Checklist: whether an ADR / `docs/` update / RLS migration is needed.
- Do not merge secrets or generated artifacts.

## Tagging / release

Until a separate library publish: **core is source-linked** (same commit as clients); optional `core/VERSION` at product tags. Store numbers (Android / iOS) and web deploy are independent. SemVer + registry for core — only when needed. Details: [ADR 0005](../decisions/0005-monorepo-tooling.md).

## Forbidden

- `git push --force` on `master` without an explicit decision.
- Commit `--no-verify` without a reason.
- Amend a pushed commit without agreement.
