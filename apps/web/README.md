# GymManager web

React + TypeScript + Vite client. Talks to Supabase with **`supabase-js` only** — no KMP/JS or Wasm binary ([ADR 0006](../../docs/decisions/0006-web-supabase-js-without-kmp-js.md)).

## Setup

```bash
pnpm install
cp ../../.env.example .env.local   # or root .env.local — set VITE_SUPABASE_*
pnpm --filter @gymmanager/web dev
```

Theme values come from generated tokens (`src/styles/tokens.css`, `src/theme/tokens.ts`). Regenerate from repo root:

```bash
pnpm tokens:generate
```
