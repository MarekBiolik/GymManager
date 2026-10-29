# Prototype artifact disposition

## Actual state (iteration 1)

On git `master` essentially only `.gitignore` was committed. The working tree still had **non-source** leftovers from the old prototype (nutrition tracker: React/Vite + Node + SQLite + OpenFoodFacts):

| Path | What it is | Decision |
|------|------------|----------|
| `web/dist/` | Built frontend (HTML/JS/CSS) | **Delete** during phase-1 cleanup; do not commit |
| `web/node_modules/` | npm deps without `package.json` in the source tree | **Delete**; do not commit |
| `web/src/` | Empty / no source files | **Delete** the directory with old `web/` or replace with new `apps/web/` |
| `server/` | Directory without sources | **Delete** |
| `data/gymmanager.db` (+ `-wal`, `-shm`) | Local prototype SQLite DB | **Keep temporarily** outside git as reference; before delete: optional schema/data export to `docs/legacy/` or a SQL file; then remove |
| `.gitignore` | Existing | **Keep and extend** for the monorepo (phase 1) |

## Rules

1. None of the above artifacts **enter** git history as “production code”.
2. New web lives in `apps/web/`, not old `web/`.
3. SQLite is not the target backend — target is Postgres (Supabase).
4. OpenFoodFacts may return as an integration — separate decision ([OQ-07](../open-questions.md)), not by resurrecting the old server.

## Execution checklist

- [x] `.gitignore` covers `node_modules/`, `dist/`, `*.db`, `.env*` (already on `master`)
- [ ] Optionally: dump `gymmanager.db` schema → `docs/legacy/prototype-schema.sql` (skipped — DB removed without export)
- [x] Removed `web/` (including `dist`, `node_modules`, empty `src`), `server/`, `data/gymmanager.db*`
- [x] Local cleanup done when tidying the branch after the docs iteration

## OQ status

[OQ-06](../open-questions.md) — **decided**; prototype artifacts removed from the working tree (were never in git).
