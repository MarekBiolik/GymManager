# Runbook: dump i restore Supabase (Free)

Operator guide for a solo maintainer. Implements the Free-plan backup policy from [ADR 0004](../decisions/0004-supabase-hosting.md) without changing hosting decisions.

**Out of scope:** Pro daily backups, PITR, automated CI cron dumps.

## When to use

| Situation | Action |
|-----------|--------|
| Risky migration / schema experiment on Cloud `dev` | Dump **before** applying |
| `dev` has personal data worth keeping | Periodic dump (e.g. weekly, or after a meaningful data week) |
| Verify you can recover | Minimal restore drill (below) once after `supabase/` exists |
| Free project was paused | Unpause in Dashboard first; dump does not replace unpausing |

## Prerequisites

1. Catalog `supabase/` in the repo, CLI installed, Docker running (`supabase db dump` runs `pg_dump` in a container).
2. Project linked: `supabase link` to Cloud `dev` ([ADR 0004](../decisions/0004-supabase-hosting.md) — one `dev` project on Free).
3. Database password known (Dashboard → Project Settings → Database), or use linked project flags so CLI prompts.
4. `psql` available for restore drills ([install notes](https://supabase.com/docs/guides/platform/migrating-within-supabase/backup-restore)).

Official CLI dump reference: [supabase db dump](https://supabase.com/docs/reference/cli/supabase-db-dump). Platform backup/restore guide: [Backup and Restore using the CLI](https://supabase.com/docs/guides/platform/migrating-within-supabase/backup-restore).

## Where to store dumps (and what never to commit)

Dumps are **outside Cloud** and **outside git**. They can contain PII (email, display name, meal/workout history tied to `user_id`).

| Do | Don't |
|----|--------|
| Keep dumps on encrypted local disk or a private password manager / personal vault path | Commit dump `.sql` files to this repo |
| Use a directory outside the workspace, e.g. `~/Backups/GymManager/` or `E:\Backups\GymManager\` | Put dumps under `supabase/` or any tracked path |
| Name files with date + project ref, e.g. `2026-10-29_dev_schema.sql` | Paste dump contents into issues, PRs, or chat |
| Delete or rotate old dumps when no longer needed | Treat Cloud Free as a backup — it is not |

Suggested local layout (not in repo):

```text
Backups/GymManager/
  YYYY-MM-DD_dev_roles.sql
  YYYY-MM-DD_dev_schema.sql
  YYYY-MM-DD_dev_data.sql
```

If a dump accidentally lands in the working tree: delete it, ensure it was never committed, and add a personal ignore if needed. Do **not** add real dump paths that contain secrets into shared docs.

Schema recovery without a data dump still works from git: `supabase/migrations/` (+ this runbook’s data dump for contents).

## Dump (linked Cloud `dev`)

Prefer the linked project (from repo root after `supabase link`). Docker must be up.

### Full logical backup (roles + schema + data)

Replace `BACKUP_DIR` with your off-repo path.

```bash
# From repo root; prompts for DB password if needed
supabase db dump --linked -f "$BACKUP_DIR/roles.sql" --role-only
supabase db dump --linked -f "$BACKUP_DIR/schema.sql"
supabase db dump --linked -f "$BACKUP_DIR/data.sql" --data-only --use-copy
```

Equivalent with an explicit URL (percent-encode special characters in the password). Prefer **direct** connection; use Session pooler if IPv6 is unavailable — see [Supabase Connect](https://supabase.com/dashboard/project/_/settings/general):

```bash
supabase db dump --db-url "$DEV_DB_URL" -f "$BACKUP_DIR/roles.sql" --role-only
supabase db dump --db-url "$DEV_DB_URL" -f "$BACKUP_DIR/schema.sql"
supabase db dump --db-url "$DEV_DB_URL" -f "$BACKUP_DIR/data.sql" --data-only --use-copy
```

Notes:

- Default schema dump **excludes** Supabase-managed internals the CLI filters; prefer `supabase db dump` over raw `pg_dump` for Cloud projects.
- `--data-only` dumps row data (includes user tables under schemas the CLI includes). Treat the file as **sensitive**.
- Optional: `--dry-run` prints the underlying `pg_dump` invocation without writing a file.

### Schema-only (migrations already in git)

When you only need a snapshot of applied DDL for comparison (not a full recovery package):

```bash
supabase db dump --linked -f "$BACKUP_DIR/schema.sql"
```

For day-to-day schema history, trust `supabase/migrations/` in git; the dump is the safety net for **data** and for drift checks.

## Checklist — before a risky migration

- [ ] Cloud `dev` is **not** paused (unpause if needed).
- [ ] Fresh dump written to off-repo `BACKUP_DIR` (at least `schema.sql` + `data.sql`; include `roles.sql` for a full drill package).
- [ ] Dump files are **not** staged for commit (`git status` clean of `*.sql` dumps).
- [ ] You know how you will apply the migration (`supabase db push` / linked workflow once scaffold exists).
- [ ] Rollback plan: restore drill path below **or** re-apply migrations + reload data from dump on a throwaway target — not “hope Cloud undoes it” (Free has no dashboard PITR).

Periodic (no migration): same dump steps when `dev` holds data you would miss.

## Minimal restore drill

Goal: prove once that a dump can populate an **empty** database. Prefer **local** so you do not consume the second Free Cloud project slot (Free allows two active projects; `dev` already uses one).

### Option A — Local (preferred)

1. Dump from linked `dev` as above into `BACKUP_DIR`.
2. From repo root: `supabase start` (optional local stack from ADR 0004).
3. Reset local DB to empty-with-migrations baseline if you use migrations:

   ```bash
   supabase db reset
   ```

   For a pure dump drill (ignore migration history), you can restore into local Postgres after start; local URL is typically:

   `postgresql://postgres:postgres@127.0.0.1:54322/postgres`

4. Restore the three files with `psql` (order matters):

   ```bash
   psql \
     --single-transaction \
     --variable ON_ERROR_STOP=1 \
     --file "$BACKUP_DIR/roles.sql" \
     --file "$BACKUP_DIR/schema.sql" \
     --command "SET session_replication_role = replica" \
     --file "$BACKUP_DIR/data.sql" \
     --dbname "postgresql://postgres:postgres@127.0.0.1:54322/postgres"
   ```

5. Spot-check:

   ```bash
   psql "postgresql://postgres:postgres@127.0.0.1:54322/postgres" -c '\dt public.*'
   ```

   Count rows on a known table if data existed (e.g. `profiles` after first migration is applied in real use).

6. Tear down when done: `supabase stop`. Delete or keep the dump per your retention habit.

If roles/schema collide with a fresh local stack, prefer: empty target → schema + data from dump, or rebuild schema from `supabase/migrations/` then load **data-only** (`data.sql`) into a DB that already matches migration history. For GymManager, **migrations in git + data dump** is the intended recovery story ([data-and-sync](../architecture/data-and-sync.md#polityka-backupów--restore)).

### Option B — Empty Cloud project (optional)

Use only if you accept using the second Free project (or a temporary project you will delete).

1. Create a new Supabase project; enable any non-default extensions your `dev` uses.
2. Dump from `dev` as above.
3. Restore with `psql` against the **new** project connection string (same three-file sequence as Option A).
4. Verify tables/row counts; then **pause or delete** the drill project so Free quotas stay clear.
5. Do **not** point production clients or committed `.env.example` at the drill project.

Dashboard “restore backup” on Free is **not** available — that path appears after Pro ([ADR 0004](../decisions/0004-supabase-hosting.md)).

## After upgrade to Pro (pointer only)

- Daily backups (7-day retention) in Dashboard → Database → Backups.
- Prefer dashboard restore for Cloud disasters; keep this CLI dump habit for exit/migration and pre-migration safety until you decide otherwise.
- PITR only when RPO &lt; ~1 day becomes a requirement.

## Related

- [ADR 0004 — Hosting Supabase i backupy](../decisions/0004-supabase-hosting.md)
- [data-and-sync — Hosting i backupy](../architecture/data-and-sync.md#hosting-i-backupy)
- [phase-1 scaffold playbook](../plans/phase-1-scaffold-playbook.md) — `supabase/` attach (`#14`)
- Issue [#9](https://github.com/MarekBiolik/GymManager/issues/9)
