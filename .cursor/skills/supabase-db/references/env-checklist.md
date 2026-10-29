# Supabase / DB environment checklist

## Required

| Tool | Expectation |
|------|-------------|
| **Supabase CLI** | Installed and on `PATH`; project uses repo `supabase/` config |
| **Docker** | Required for `supabase start` (local Postgres/Auth stack) |
| **Secrets** | `SUPABASE_URL` + anon key in gitignored `.env*` / password manager; **never** commit `service_role` |

## Optional

| Tool | When |
|------|------|
| Linked Cloud **`dev`** | `supabase link` for apply/dump against the shared dev project (ADR 0004) |
| GitHub Environments | After staging/prod split — see [staging/prod spec](../../../docs/specs/supabase-staging-prod-split.md) |

## Typical commands

Prefer root scripts when they exist; otherwise:

```text
supabase start
supabase db lint
supabase migration list
supabase db dump   # see dump/restore runbook before risky Cloud changes
```

Exact flags follow current CLI `--help` and the runbook — do not invent flags from memory when unsure.

## Secrets handling

| OK in client / repo examples | Never in clients or git |
|------------------------------|-------------------------|
| Project URL, anon (public) key placeholders in `.env.example` | `service_role`, DB passwords, personal access tokens |
