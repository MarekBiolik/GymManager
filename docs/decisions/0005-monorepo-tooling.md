# ADR 0005 — Monorepo: layout, tooling, and core versioning

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ OQ-04 closing session)

## Context

The repository holds **one product** on three clients (iOS, Android, web) with a shared KMP core ([ADR 0001](0001-shared-core-kmp.md), [ADR 0003](0003-native-ui-per-platform.md)). We must decide:

1. how we split directories and builds (Gradle vs Node),
2. whether we add a separate monorepo orchestrator (Nx / Turborepo),
3. how we version `core` relative to clients (store builds, web deploy).

A bad tool = dual build systems without a clear boundary, or premature complexity (Nx across three ecosystems). No versioning decision complicates CI and release in phases 1/6.

## Decision

**Hybrid monorepo without Nx/Turborepo:**

1. **Gradle multi-project** covers `core/` (KMP) and `apps/android/` (Compose). iOS consumes the KMP artifact (XCFramework / local SPM) built from the same Gradle; the Xcode project lives in `apps/ios/`.
2. **`apps/web/`** is a separate **pnpm** workspace + Vite + React/TS — outside the Gradle tree. Web → Supabase via `supabase-js`, **without** KMP→JS/Wasm ([ADR 0006](0006-web-supabase-js-without-kmp-js.md)); no parallel TS domain.
3. **Root** holds thin orchestration scripts (`package.json` / Makefile / `scripts/`) calling Gradle, pnpm, and Supabase CLI — no second “meta-builder”.
4. **`core` versioning:** until a separate library publish, core is **source-linked in the same commit** as clients (Gradle project dependency / local framework). SemVer for core + optional Maven/SPM publish — only when an external consumer or independent release cycle appears; until then a `core/VERSION` file (or `gradle.properties`) synced at product release tags is enough. Store build numbers (Android `versionCode`, iOS `CFBundleVersion`) and web deploys are **independent** of core SemVer.

Target tree and ownership: [overview](../architecture/overview.md). Tool stack: [tech-stack](../architecture/tech-stack.md).

## Alternatives considered

1. **One mega-Gradle (including web via plugin)** — one tool. Rejected: web DX (Vite, ESLint, pnpm) fits Gradle poorly; plugin maintenance cost without gain.
2. **Nx / Turborepo over the whole repo** — task cache, dependency graph. Rejected at start: three toolchains (Gradle, Xcode, Node); Nx/Turbo mainly help JS. May consider **later only for `apps/web` + TS packages** if web grows.
3. **Polyrepo** (separate repos for core / each client) — clean version boundaries. Rejected: solo/small team, frequent cross-cutting changes; ADRs and scaffolding in one place matter more than independent releases.
4. **Publish core to Maven Central / private registry from day 1** — hard SemVer. Rejected as premature: one product, one commit; source-link is simpler for CI and local DX.
5. **Hybrid Gradle (core+Android) + pnpm (web) + root scripts** — chosen: natural for KMP/Android, standard for React, minimal orchestration.

## Consequences

- Positive: clear toolchain split; Android and core share one Gradle; web keeps mature DX; root scripts suffice for “build all” / CI; no Nx vendor lock-in.
- Negative / costs: two (three with Xcode) build systems to learn; root scripts maintained by hand; iOS needs a framework-export step from Gradle before/alongside Xcode.
- Later: revisit KMP→JS/Wasm only per ADR 0006 conditions; SemVer/publish core only when needed; optionally Turborepo under web only.
- Scaffold playbook: [phase-1-scaffold-playbook](../plans/phase-1-scaffold-playbook.md) ([issue #7](https://github.com/MarekBiolik/GymManager/issues/7)); implementation [#14](https://github.com/MarekBiolik/GymManager/issues/14).

### First scaffold implementation scope (phase 1) vs later

| In first scaffold scope | Deliberately later |
|-------------------------|--------------------|
| Root: `settings.gradle.kts` / wrapper, `build`/`test`/`lint` scripts | Nx / Turborepo |
| `core/` as KMP module (**only** Android + iOS targets; **no** JS/Wasm) | JS/Wasm target / publish core to a registry |
| `apps/android/` depending on `:core` | Independent core vs client versioning |
| `apps/ios/` Xcode project + framework wiring instructions | Full SPM package distribution |
| `apps/web/` pnpm + Vite + `supabase-js` (no KMP binary) | Multi-package JS monorepo; consuming core from web |
| `supabase/` CLI config + place for migrations | Multi-environment release automation |
| `docs/` (already exists) | — |
| `core/VERSION` or `gradle.properties` entry (placeholder) | Store release / tag policy (phase 6) |

## Related

- [overview — repo structure](../architecture/overview.md)
- [tech-stack](../architecture/tech-stack.md)
- [shared-core](../architecture/shared-core.md)
- [clients](../architecture/clients.md)
- [OQ-04](../open-questions.md) (decided)
- [ADR 0001](0001-shared-core-kmp.md), [ADR 0003](0003-native-ui-per-platform.md)
- [roadmap — phase 1](../roadmap.md)
