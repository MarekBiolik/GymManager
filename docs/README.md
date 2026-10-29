# GymManager — documentation

Project documentation index. These documents are the source of truth for architecture, domain, and conventions; code is secondary to the decisions recorded here. Docs describe **current** state — see [docs lifecycle](conventions/docs-lifecycle.md).

## Start here

1. [Product vision](00-vision.md) — what we build, scope, non-goals
2. [Roadmap](roadmap.md) — work phases and when each area gets refined
3. [Open questions](open-questions.md) — areas to refine in later iterations

## Plans (active only)

- [Phase 1 — monorepo scaffold playbook](plans/phase-1-scaffold-playbook.md) — active until [#14](https://github.com/MarekBiolik/GymManager/issues/14) lands; then delete this playbook and keep code + living conventions

## Architecture

- [Overview](architecture/overview.md) — high-level, diagram, repo layout
- [Tech stack](architecture/tech-stack.md)
- [Shared core (KMP)](architecture/shared-core.md)
- [Clients](architecture/clients.md) — iOS / Android / web
- [Data and sync](architecture/data-and-sync.md)
- [Shared Postgres invariants](architecture/postgres-invariants.md) — RLS / CHECK vs core/TS (after ADR 0006)
- [Authentication](architecture/auth.md)

## Decisions (ADR)

- [ADR template](decisions/adr-template.md)
- [ADR 0001 — Shared core: Kotlin Multiplatform](decisions/0001-shared-core-kmp.md)
- [ADR 0002 — Backend: Supabase](decisions/0002-backend-supabase.md)
- [ADR 0003 — Native UI per platform](decisions/0003-native-ui-per-platform.md)
- [ADR 0004 — Supabase hosting and backups](decisions/0004-supabase-hosting.md)
- [ADR 0005 — Monorepo: layout, tooling, and core versioning](decisions/0005-monorepo-tooling.md)
- [ADR 0006 — Web: supabase-js without KMP→JS/Wasm](decisions/0006-web-supabase-js-without-kmp-js.md)
- [ADR 0007 — Auth providers and minimum PII](decisions/0007-auth-providers-minimum-pii.md)
- [ADR 0008 — Sync strategy: online-first + read cache](decisions/0008-sync-strategy-online-first.md)
- [ADR 0009 — Food data source: own foods first](decisions/0009-food-data-source-own-first.md)
- [ADR 0010 — Design tokens SSOT and cross-platform generation](decisions/0010-design-tokens-ssot.md)

## Specifications (design / UX)

- Design-tokens contract and generation pipeline — SPEC ONLY ([#61](https://github.com/MarekBiolik/GymManager/issues/61)); follows [ADR 0010](decisions/0010-design-tokens-ssot.md)
- Fitness/Gym visual style — SPEC ONLY ([#62](https://github.com/MarekBiolik/GymManager/issues/62)); depends on tokens contract

## Specifications (nutrition — post-MVP)

- [OpenFoodFacts import/search hybrid](specs/openfoodfacts-import-hybrid.md) — SPEC ONLY ([#26](https://github.com/MarekBiolik/GymManager/issues/26)); after own-foods MVP / ADR 0009; no OFF client in this ticket

## Domain

- [Glossary](domain/glossary.md)
- [Data model (outline)](domain/data-model.md)

## Operator runbooks

- [Supabase dump and restore (Free)](runbooks/supabase-dump-restore.md) — periodic `db dump`, no PII in git, restore drill ([ADR 0004](decisions/0004-supabase-hosting.md), [#9](https://github.com/MarekBiolik/GymManager/issues/9))

## Specifications (SQL / migrations / ops / IdP — draft)

- [First migration: profiles + baseline RLS](specs/first-migration-profiles-rls.md) — SPEC ONLY; apply = [#21](https://github.com/MarekBiolik/GymManager/issues/21); draft SQL in `docs/specs/sql/` (**NOT APPLIED**)
- [Supabase staging/prod environment split](specs/supabase-staging-prod-split.md) — SPEC ONLY ([#10](https://github.com/MarekBiolik/GymManager/issues/10)); aligns with [ADR 0004](decisions/0004-supabase-hosting.md); no Dashboard provisioning in this ticket
- [IdP checklist: Google + Apple + Supabase redirects](specs/idp-config-checklist.md) — docs only ([#12](https://github.com/MarekBiolik/GymManager/issues/12)); no SDK code / secrets
- [Hello sync / read-cache (phase 2)](specs/hello-sync-read-cache.md) — client contract under [ADR 0008](decisions/0008-sync-strategy-online-first.md); [#19](https://github.com/MarekBiolik/GymManager/issues/19); no application code
- [Offline-write queue and conflict revisit](specs/offline-write-queue.md) — SPEC ONLY; **post-MVP** / not phase 1 ([#20](https://github.com/MarekBiolik/GymManager/issues/20)); no implementation now

## Conventions

- [Documentation lifecycle](conventions/docs-lifecycle.md) — current state only; delete executed plans
- [Coding standards](conventions/coding-standards.md)
- [Git workflow](conventions/git-workflow.md)
- [Testing](conventions/testing.md)
- [CI/CD](conventions/ci-cd.md)
- [Agent development environment and delivery flow](conventions/agent-dev-environment.md) — areas, core-first → parallel platforms → test; Cursor-native orchestration; simulator strategy

## Documentation working rules

- New architectural decisions → ADR in `docs/decisions/`.
- Ambiguities and deferred topics → `open-questions.md`, not silent fixes in code.
- Refining an area = a separate iteration (plan + docs update), then scaffolding/code.
- Finished plans/playbooks → delete; do not archive process sediment in `docs/`.
