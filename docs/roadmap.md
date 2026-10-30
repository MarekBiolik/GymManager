# Roadmap

Work phases. Each phase ends with a documentation (or ADR) update before going deep into code.

## Phase 0 — Documentation foundations

**Goal:** outline vision, architecture, domain, conventions, and open questions.

**Output:** the `docs/` tree (this document set).

**Status:** done — ADR 0001–0003 (+ later 0004–0007 in OQ iterations); open-questions and base conventions exist.

---

## Phase 1 — Monorepo and infrastructure scaffolding

**Goal:** empty, building projects + attached Supabase (`dev`) + CI skeleton + **design-tokens foundation wired for apps to consume**.

**Status:** scaffold [#14](https://github.com/MarekBiolik/GymManager/issues/14) + first migration [#21](https://github.com/MarekBiolik/GymManager/issues/21) (playbook #7 executed and removed per docs-lifecycle). Maintainer still links Cloud `dev` and runs `npx supabase db push` per [supabase/APPLY_DEV.md](../supabase/APPLY_DEV.md).

**Design tokens:** [ADR 0010](decisions/0010-design-tokens-ssot.md), [design-tokens contract](specs/design-tokens.md), [Fitness/Gym visual style](specs/visual-style-fitness-gym.md) — `design/` generator + app theme wiring land with the scaffold.

Scope:

- Layout per [ADR 0005](decisions/0005-monorepo-tooling.md): Gradle (`core`, `apps/android`), Xcode (`apps/ios`), pnpm (`apps/web`), `supabase/`, `design/`, root scripts
- Web uses `supabase-js` only — **no** KMP→JS/Wasm ([ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md))
- Attach Supabase Cloud `dev` ([ADR 0004](decisions/0004-supabase-hosting.md)); local secrets / GitHub Secrets; dumps → [#9](https://github.com/MarekBiolik/GymManager/issues/9); `staging`/`prod` split before public beta → [spec](specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)) (not in #14)
- First schema migration: **spec** [first-migration-profiles-rls](specs/first-migration-profiles-rls.md) ([#6](https://github.com/MarekBiolik/GymManager/issues/6)); **apply** = [#21](https://github.com/MarekBiolik/GymManager/issues/21) (after OQ-03 → [ADR 0008](decisions/0008-sync-strategy-online-first.md))
- CI skeleton on PR (Gradle core/android + pnpm web + core tests + token regen-clean when generator exists) — details in the playbook / [ci-cd](conventions/ci-cd.md)
- Auth: **no** full OAuth smoke in the scaffold; IdP checklist [spec](specs/idp-config-checklist.md) ([#12](https://github.com/MarekBiolik/GymManager/issues/12)), login UI → phase 2

**Done when (before deep phase-2 code):**

1. Playbook merged; [#14](https://github.com/MarekBiolik/GymManager/issues/14) merged — tree builds locally and on CI.
2. `supabase/` + link to Cloud `dev`; no secrets in git.
3. Spec [#6](https://github.com/MarekBiolik/GymManager/issues/6) / [docs/specs/first-migration-profiles-rls.md](specs/first-migration-profiles-rls.md) merged and SQL migration 1:1 with the spec in `supabase/migrations/` + on `dev` ([#21](https://github.com/MarekBiolik/GymManager/issues/21)).
4. No JS/Wasm target in `core`; web does not consume a KMP binary.
5. Dump (#9) / IdP (#12) follow-ups left open on purpose — they do not block closing layout alone; #6+#21 block “phase 1 complete”.

**Closed before empty scaffold code starts:** OQ-05 → ADR 0004; OQ-04 → ADR 0005; OQ-01 → ADR 0006; OQ-03 → ADR 0008; playbook #7; migration spec #6.  
**Does not block empty #14 start:** SQL apply (#21) — schema content is in the spec; #14 keeps `migrations/` empty until #21.

---

## Phase 2 — Auth + sync baseline

**Goal:** user signs in, data is RLS-isolated, basic sync works on all clients.

Scope:

- Implement sign-in per [ADR 0007](decisions/0007-auth-providers-minimum-pii.md) / [auth](architecture/auth.md) (spec closed; here code + IdP config)
- Account deletion (cascade / Edge Function) per the outline in auth.md
- Sync per [ADR 0008](decisions/0008-sync-strategy-online-first.md): online writes + read cache + LWW; thin “hello sync” UI — contract [hello-sync-read-cache](specs/hello-sync-read-cache.md) ([#19](https://github.com/MarekBiolik/GymManager/issues/19))
- Shared repositories in core (mobile) + equivalent `supabase-js` calls (web)

**Dependent open questions:** (no blocking OQ-02/03). Auth/PII: **decided** ([OQ-02](open-questions.md) → ADR 0007). Sync/offline: **decided** ([OQ-03](open-questions.md) → ADR 0008). Web stays on `supabase-js` ([ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md)); offline-write parity with core only on revisit / [offline-write-queue](specs/offline-write-queue.md) ([#20](https://github.com/MarekBiolik/GymManager/issues/20)).

---

## Phase 3 — Nutrition module (MVP)

**Goal:** personal Fitatu-like tracker — goals/settings, own foods + flexible servings, weekly diary with meal sections, OFF search/import + barcode, recipes, charts, water, copy meal/day — Android + web.

**Data source:** own `foods` as diary SoT; **hybrid** OFF search/import + barcode in phase 3 ([ADR 0011](decisions/0011-food-data-hybrid-in-phase-3.md), supersedes ADR 0009 phase boundary). Contract: [nutrition-module](specs/nutrition-module.md); OFF detail: [openfoodfacts-import-hybrid](specs/openfoodfacts-import-hybrid.md). Starter catalog empty unless [OQ-10](open-questions.md) seed is needed.

**Status:** done — feature epics under [#91](https://github.com/MarekBiolik/GymManager/issues/91) shipped (settings → foods → diary → OFF → barcode via paste/lookup → recipes → charts → water/copy). CameraX/camera barcode scan deferred out of Phase 3 MVP.

---

## Phase 4 — Training module (MVP)

**Goal:** exercises, sessions, sets, workout history.

---

## Phase 5 — Measurements module (MVP)

**Goal:** body measurements over time, simple charts/aggregations (Postgres).

---

## Phase 6 — UX integration “one app”

**Goal:** coherent navigation between modules, polish, App Store / Play Store / web hosting.

**Owns:** module-switching UX ([OQ-08](open-questions.md)), store polish, coherent nav — **not** the first design-token decision ([ADR 0010](decisions/0010-design-tokens-ssot.md) / Phase 1). Theming / light-mode / alternate-style policy: [OQ-09](open-questions.md).

---

## Phase 7 — AI (later)

**Goal:** skills / agents / workflows around stabilized infrastructure and domain.

Deliberately **after** phases 0–6. We do not design the AI stack here.

---

## How we work with the roadmap

1. Before a phase starts: short plan + refine related items from [open-questions](open-questions.md).
2. Durable decisions → ADR.
3. Code only after the area’s documentation is updated.
4. The roadmap is not a date contract — it is dependency order.
