---
name: web-react
description: >-
  Implement GymManager web UI in React + TypeScript + Vite with supabase-js:
  thin presentation, no KMP binary, no parallel TS domain. Use when editing
  apps/web/, Vite/React routes, supabase-js client wiring, or the user says
  web / React / Vite.
---

# Web (React)

Own `apps/web/`: React + TypeScript + Vite UI talking to Supabase via **`supabase-js` only**. Day-1 interop: [ADR 0006](../../../docs/decisions/0006-web-supabase-js-without-kmp-js.md). Client role: [clients](../../../docs/architecture/clients.md). Shared rules with mobile: [postgres-invariants](../../../docs/architecture/postgres-invariants.md). Coding: [coding-standards](../../../docs/conventions/coding-standards.md).

**Theme:** consume generated **CSS variables + TypeScript tokens** (theme hook / CSS). Do **not** hardcode palette hex — change values via SSOT + regenerate (`design-tokens` skill). UX flows/IA → `ui-ux`. See [references/theming.md](references/theming.md).

Env: [references/env-checklist.md](references/env-checklist.md).

## Workflow

Copy and track:

```
Web slice:
- [ ] 1. Confirm DB / auth contract
- [ ] 2. Thin UI + supabase-js
- [ ] 3. Types and env
- [ ] 4. Verify build
- [ ] 5. Hand-off
```

### 1. Confirm DB / auth contract

Web does **not** consume the KMP binary. Shared integrity/isolation lives in Postgres (RLS/CHECK). If the slice needs new shared rules, land them with `supabase-db` (and mobile `core-kmp` when mobile shares the behavior) before inventing TS “services” that duplicate domain.

**Done when:** tables/policies/session contract for this slice are known (spec or migration).

### 2. Thin UI + supabase-js

**Allowed:** routing, layout, theme hooks that read **generated** CSS vars / TS tokens, session glue via `supabase-js`, row/DTO → view mapping, light form validation that **mirrors** DB/core.

**Forbidden:**

- Hardcoding brand/surface hex — bind to generated `--color-*` / `tokens` instead
- KMP-JS/Wasm imports or assuming a shared Kotlin domain on web
- Parallel TypeScript domain (use cases, business aggregations, “domain services”)
- Treating TS-only validation as a security boundary
- Bypassing RLS or using `service_role` in the browser

Prefer functional components, TypeScript strict, no unjustified `any`.

**Done when:** UI calls Supabase through the thin client helper; no new business-rule module in TS.

### 3. Types and env

- Prefer types generated from the Supabase schema when the project has that pipeline; otherwise hand types that mirror the contract — still not a domain layer
- URL + anon/publishable key from `import.meta.env` (Vite); placeholders in `.env.example` only
- Never ship `service_role`

**Done when:** env wiring is git-safe; types track the schema contract.

### 4. Verify build

```text
pnpm install
pnpm --filter <web> build
```

(Exact filter/script names follow root `package.json` / `pnpm-workspace.yaml`.) Lint when ESLint is configured. Browser smoke via browser tools when useful — not a substitute for RLS tests.

**Done when:** production Vite build succeeds for the change.

### 5. Hand-off

Short status: routes/components touched, supabase calls used, build result. Coordinate with Android/iOS if the product flow must match. PR/merge only if the user asked.

**Done when:** web AC for the slice are tickable.

## Guardrails

- `supabase-js` only — no KMP on web (ADR 0006)
- No parallel TS domain; shared invariants → Postgres
- No parallel palette — tokens via `design-tokens` SSOT + regenerate
- No `service_role` in web
- Prefer strong model for auth/session edge cases; cheap helpers for pnpm/search
