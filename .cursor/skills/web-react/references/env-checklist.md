# Web environment checklist

## Required

| Tool | Expectation |
|------|-------------|
| **Node.js** | LTS (or version pinned in repo docs/engines when present) |
| **pnpm** | Workspace package manager ([ADR 0005](../../../docs/decisions/0005-monorepo-tooling.md)) |
| **Env files** | gitignored `.env.local` (or equivalent) with `VITE_SUPABASE_URL` + anon/publishable key |

## Typical commands

```text
pnpm install
pnpm --filter <web> build
pnpm --filter <web> dev
```

Prefer root scripts (`package.json`) when they wrap the same actions. Exact package name follows `pnpm-workspace.yaml`.

## Secrets

| OK | Never |
|----|--------|
| Project URL, anon/publishable key in local env | `service_role`, management tokens in `apps/web` or git |
