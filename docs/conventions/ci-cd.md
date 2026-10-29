# CI/CD

Principle skeleton. Concrete jobs and enablement order: [phase-1 playbook](../plans/phase-1-scaffold-playbook.md) (step F) — implementation in [#14](https://github.com/MarekBiolik/GymManager/issues/14).

## CI goals on PR

Per [ADR 0005](../decisions/0005-monorepo-tooling.md) the pipeline joins **separate toolchains** (Gradle, pnpm, optionally Xcode), not one monorepo orchestrator. **No** KMP→JS/Wasm build ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)).

1. Build `core` via Gradle (at least Android/JVM target; iOS / XCFramework when a macOS runner is available — not required day one on Linux).
2. Lint / format check (Kotlin via Gradle; TS via pnpm; Swift when possible).
3. Unit tests for `core` (Gradle) — the task must exist even if the suite is empty.
4. Supabase migration validation (dry-run / `supabase db lint`) — when the repo has ≥1 migration.
5. Web build via pnpm (`apps/web` + Vite).

A PR fails when `core`, `apps/android`, or `apps/web` build fails, or when core tests fail. Root scripts should expose the same commands locally and in CI.

## CD (direction)

| Artifact | Distribution |
|----------|--------------|
| Web | Static hosting (e.g. Cloudflare Pages / Vercel / Supabase hosting) — choose later |
| Android | Play Console (internal track first) |
| iOS | TestFlight → App Store (Apple Developer) |
| Supabase | Migrations: **staging first, then prod** (never the reverse); never “click in dashboard” as source of truth — [split spec](../specs/supabase-staging-prod-split.md) |

## Secrets

- In CI: secrets store (e.g. GitHub Secrets).
- **At start (phase 1):** one Supabase Cloud project = `dev` ([ADR 0004](../decisions/0004-supabase-hosting.md)) — a flat Secrets set is enough.
- **After the split (before public beta):** GitHub Environments **`staging`** and **`production`**, each with its own `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and (admin/migration jobs only) `SUPABASE_SERVICE_ROLE_KEY` / CLI token — table in [split spec](../specs/supabase-staging-prod-split.md).
- `service_role` only in Secrets / Environments / local admin — never in clients or logs.
- No secrets in build logs.
- Protection rules: deploy/migrate `production` only from `master` (or a release tag) + optional required reviewer.

## OTA / hotfix

None for now. Fast web fixes are easier than mobile (store review).

## Pipeline definition of done (phase 1)

- [ ] Workflow on PR (GitHub Actions): Gradle (`core` + `apps/android`) + `core` tests + pnpm build `apps/web`; lint when tools are in the repo
- [ ] No job requiring KMP-JS/Wasm
- [ ] Branch protection on `master` when CI exists and is green
- [ ] Same checks documented locally (root scripts + playbook steps D/F)
