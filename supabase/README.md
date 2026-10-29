# Supabase

CLI config for GymManager. **Cloud Free project = `dev`** ([ADR 0004](../docs/decisions/0004-supabase-hosting.md)).

## Maintainer checklist

- [ ] Cloud `dev` project exists and is not paused
- [ ] URL + anon key in local `.env` / `.env.local` (see root `.env.example`)
- [ ] `service_role` only in a password manager — **never** in git or clients
- [ ] Before a risky migration: dump (runbook → [#9](https://github.com/MarekBiolik/GymManager/issues/9))

## Link Cloud `dev`

```bash
npx supabase login
npx supabase link --project-ref <YOUR_DEV_PROJECT_REF>
```

Project ref and access tokens stay local (CLI stores them outside the repo). Do not commit `.temp/` or linked secrets.

## Migrations

- Phase 1 scaffold (#14): this folder may stay **empty** (only `.gitkeep`).
- First schema (`profiles` + RLS): [#21](https://github.com/MarekBiolik/GymManager/issues/21) copies [docs/specs/sql/0001_profiles_baseline_rls.sql](../docs/specs/sql/0001_profiles_baseline_rls.sql) 1:1 into `migrations/`.
