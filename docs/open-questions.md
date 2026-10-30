# Open questions

Areas still to refine, plus short `decided` pointers to living ADRs/specs. Close items with an ADR or docs update—not a process post-mortem. Drop OQs that leave no lasting decision (cleanup chores belong in git history only).

Statuses: `open` | `in progress` | `decided` (with link to ADR/docs).

---

## OQ-01 — Web ↔ shared core (KMP) interop

**Status:** decided — [ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md)  
**Why it matters:** KMP naturally ties Android and iOS together; web (JS/Wasm) is harder. A bad choice = duplicated domain logic or fragile interop.

**Decision (day 1):** option **B** — web → `supabase-js` + thin TS; **no** KMP→JS/Wasm in the scaffold. Guardrails and revisit criteria: ADR 0006 + [clients](architecture/clients.md) / [shared-core](architecture/shared-core.md). Operational split of DB vs core/TS rules: [postgres-invariants](architecture/postgres-invariants.md) (issue #17).

**Options (considered):**

1. Core compiled to JS/Wasm, consumed by React/TS.
2. Web talks directly to Supabase via `supabase-js`; thin logic in TS (duplication risk vs core) — **chosen for start**.
3. Hybrid: domain/validation in shared (if feasible), web networking separate.

---

## OQ-02 — Sign-in method and minimum PII

**Status:** decided — [ADR 0007](decisions/0007-auth-providers-minimum-pii.md), [auth](architecture/auth.md)  
**Decision:** Google (Android/iOS/web) + Sign in with Apple on iOS; minimum PII (`profiles` without email/name); account-deletion outline in auth.md. Beyond MVP: magic link, anon+linking, email+password.

---

## OQ-03 — Offline / sync / conflict strategy

**Status:** decided — [ADR 0008](decisions/0008-sync-strategy-online-first.md), [data-and-sync](architecture/data-and-sync.md)  
**Decision:** online-first + local read cache; SoT = Postgres; writes online-only in MVP; conflicts = LWW per row (`updated_at`), one rule for all entity types; Realtime optional; offline-write later — [spec](specs/offline-write-queue.md) ([#20](https://github.com/MarekBiolik/GymManager/issues/20)).  
**Schema (`updated_at`, RLS):** [first migration spec](specs/first-migration-profiles-rls.md) (#6); apply = [#21](https://github.com/MarekBiolik/GymManager/issues/21).

---

## OQ-04 — Monorepo tooling and versioning

**Status:** decided — [ADR 0005](decisions/0005-monorepo-tooling.md), [overview](architecture/overview.md)  
**Decision (summary):** Gradle multi-project for `core` + `apps/android` (framework export for iOS); `apps/web` on pnpm/Vite outside Gradle; orchestration = thin root scripts (no Nx/Turborepo); core source-linked in the same commit, SemVer/publish later.

---

## OQ-05 — Supabase hosting and backups

**Status:** decided — [ADR 0004](decisions/0004-supabase-hosting.md), [data-and-sync](architecture/data-and-sync.md#hosting-and-backups)  
**Decision:** Supabase Cloud Free to start → Pro when auto-backups / no-pause are needed; one `dev` project (+ optional local CLI); secrets outside the repo; on Free, periodic `db dump`.  
**`staging`/`prod` split (follow-up):** [spec](specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)) — when, naming, Environments, staging→prod migrations, Pro criteria for `prod`; project provisioning later.

---

## OQ-07 — Food data source

**Status:** decided — [ADR 0009](decisions/0009-food-data-source-own-first.md) (own foods SoT); phase-3 hybrid boundary revised by [ADR 0011](decisions/0011-food-data-hybrid-in-phase-3.md)  
**Decision:** Own `foods` under RLS remain diary SoT; macro snapshot in `food_entries`. Phase 3 includes OFF search/import → copy into `foods` (not live OFF SoT). See ADR 0011.

**Options (considered under ADR 0009):**

1. Own foods only (no hybrid path).
2. OFF-only (live API).
3. Hybrid from day 1.
4. Own first + hybrid later — **chosen in ADR 0009**; **revised** by ADR 0011 to hybrid **in** phase 3.

---

## OQ-10 — Basics food seed

**Status:** open  
**Context:** Phase 3 starts with an **empty** own `foods` catalog ([ADR 0011](decisions/0011-food-data-hybrid-in-phase-3.md)). OFF import may under-serve Polish staples (vegetables, fruit, bread). Before calling the nutrition module “done”, decide whether to add a small **hardcoded per-user seed** of basics.

**Why it matters:** empty catalog + weak OFF coverage = high friction on first diary day.

**Options (known):**

1. **Stay empty** — rely on manual create + OFF only.
2. **Per-user seed on first nutrition open** — copy ~30–50 staples into the user’s `foods` (not a shared global table).
3. **Opt-in “Load basics”** in settings — same data, user-triggered.

**Closing phase:** before end of phase 3 (after OFF search is usable enough to judge coverage).

**Links:** [ADR 0011](decisions/0011-food-data-hybrid-in-phase-3.md), [nutrition-module](specs/nutrition-module.md), [data-model](domain/data-model.md#foods).

---

## OQ-08 — Module-switching UI/UX

**Status:** open  
**Context:** one app, several areas. Navigation (tabs, drawer, “mode switch”) — detail for phase 6; does not block foundations.

---

## OQ-09 — Theming and style-swap governance

**Status:** open  
**Context:** [ADR 0010](decisions/0010-design-tokens-ssot.md) + [design-tokens](specs/design-tokens.md) make visual values swappable via SSOT. The first style is [Fitness/Gym OLED dark](specs/visual-style-fitness-gym.md). Still open: whether/when to ship **light mode**, how **default vs alternate** styles are chosen, and **who** may change the active theme (user setting vs build-time / release-only).

**Why it matters:** light mode and multi-style policy affect token files, generator mode switching, settings UI, and store screenshots — without blocking dark-first MVP UI.

**Options (known):**

1. **Dark-only through MVP** — ship OLED Fitness/Gym only; add light semantic map later (phase 6 or post-MVP).
2. **Dark default + optional light** — user toggle; both semantic maps maintained from day of first settings screen.
3. **Build-time / flavor styles only** — no in-app theme picker; alternate styles are release or white-label forks via token sets.
4. **In-app style catalog** — user picks among named themes (Fitness/Gym + future); higher settings and QA cost.

**Closing phase:** before or during **phase 6** polish (or earlier if a second style is required for launch). Does not block Phase 1 tokens contract or dark-first scaffold theme wiring.

**Links:** [ADR 0010](decisions/0010-design-tokens-ssot.md), [design-tokens](specs/design-tokens.md) ([#61](https://github.com/MarekBiolik/GymManager/issues/61)), [visual-style-fitness-gym](specs/visual-style-fitness-gym.md) ([#62](https://github.com/MarekBiolik/GymManager/issues/62)).

---

## How to add new questions

1. Assign id `OQ-NN`.
2. Status `open`, short context, options if known, closing phase.
3. After decision: status `decided` + link to ADR or docs section.
