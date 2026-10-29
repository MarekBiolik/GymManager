---
name: platform-delivery
description: >-
  Deliver a GymManager slice core-first then parallel platforms then tests:
  shared KMP/DB contract before Android/web/iOS UI, on-demand subagents,
  skip-gate, model-budget. Use when implementing a multi-platform feature,
  executing an approved plan that spans core and clients, fanning out
  platform work, or the user says core-first / platform delivery.
---

# Platform delivery

Lead the open chat: plan, decide, dispatch. Implement **core first**, then **parallel platforms**, then **tests**. Convention of record: [docs/conventions/agent-dev-environment.md](../../../docs/conventions/agent-dev-environment.md). Existing issue → PR → commit rules stay in force.

## Workflow

Copy and track:

```
Platform delivery:
- [ ] 1. Intake gate
- [ ] 2. Issues ready
- [ ] 3. Core slice
- [ ] 4. Parallel platforms
- [ ] 5. Tests
- [ ] 6. Hand-off
```

### 1. Intake gate

If the request still needs facts, a throwaway probe, or unresolved design branches, run `research`, `prototype`, and/or `interview` **before** coding. Otherwise skip.

**Done when:** plan is agreed (or the issue already encodes an agreed plan).

### 2. Issues ready

If the work is not yet sliced, run `to-issues`. AC checkboxes on the issue are the done-list.

**Skip gate (ask if unclear):** user may skip a platform, skip tests, or both. Record skips in the issue/PR; do not invent them.

**Done when:** implementable issue(s) exist with AC, or user pointed at an existing issue.

### 3. Core slice

Land the shared seam first:

- KMP domain / use cases / repositories / sync in `core/`, and/or
- Postgres + RLS contract under `supabase/` when the slice needs schema

Platform UIs wait until this seam is mergeable or clearly staged for the same PR stack.

Prefer a strong model for hard domain, sync, or RLS calls. Dispatch search / mechanical edits to cheaper helpers when useful.

**Done when:** core (and required DB contract) satisfy the issue’s core AC; thin UI has a stable API/contract to call.

### 4. Parallel platforms

Fan out with on-demand `Task` subagents when write ownership is disjoint:

| Area | Path | Today |
|------|------|--------|
| Android | `apps/android/` | Full UI over core |
| Web | `apps/web/` | Thin React + `supabase-js` (ADR 0006) |
| iOS | `apps/ios/` | Build-spec / docs only until Mac access (#49) |

Use area skills when they exist; otherwise frame the subagent with the area row above and [clients](../../../docs/architecture/clients.md). One writer per path; no persistent advisor/inbox files.

**Done when:** each non-skipped platform meets its AC (iOS = docs/wiring only unless Mac access is decided).

### 5. Tests

Unless the user skipped tests:

1. Core unit tests for new rules
2. Web build / targeted checks
3. Android smoke when emulator runner skill exists and an emulator is available
4. RLS / migration checks when the slice touched `supabase/`

Prefer cheap models for running builds/tests; keep the lead on failures that need judgement.

**Done when:** required checks are green, or skips are explicit on the issue.

### 6. Hand-off

- Short status on the issue or PR: done / waiting on / next step
- Open or update PR with `pr` when the user wants a PR (never merge unless asked)
- Tick satisfied AC checkboxes via `gh`

**Done when:** user has a clear next action (review draft, merge ask, or next issue).

## Skill routing

Load the matching project skill when the slice touches its surface. Delivery order stays **core-first → parallel platforms → tests** (this skill’s workflow above). Deep topics live in each skill’s `references/` — see [skill references convention](../../../docs/conventions/skill-references.md).

| Skill | When to load | `references/` (when present) |
|-------|--------------|------------------------------|
| `core-kmp` | `core/` domain, use cases, sync, commonTest | `env-checklist.md` |
| `supabase-db` | `supabase/` migrations, RLS, Auth schema | `env-checklist.md` |
| `design-tokens` | `design/tokens/`, generator, theme outputs, regen-clean | `token-architecture.md`, `naming.md`, `platform-outputs.md` |
| `ui-ux` | Flows, IA, a11y contracts, cross-client UX intent | `flows.md`, `a11y-floor.md`, `env-checklist.md` |
| `android-compose` | `apps/android/` Compose UI / wiring | `theming.md`, `state-management.md`, `performance.md`, `a11y.md`, `env-checklist.md` |
| `web-react` | `apps/web/` React / Vite / supabase-js | `theming.md`, `env-checklist.md` |
| `ios-swiftui` | `apps/ios/` SwiftUI / wiring docs (build-spec until #49) | `theming.md`, `env-checklist.md` |
| `android-emulator` | AVD/adb install/launch smoke | `env-checklist.md` |
| `qa-testing` | Ordered test gates for a slice | `env-checklist.md` |
| `platform-delivery` | Multi-platform fan-out (this skill) | — |

Appearance **values** → `design-tokens`; UX **flows** → `ui-ux`; platform **consumption** of generated tokens → platform skills. Do not micro-split skills for every deep topic — add a `references/` file instead.

## Model budget

| Work | Preference |
|------|------------|
| Search, builds, test runs, mechanical edits | Cheap / fast helpers via `Task` |
| Domain seams, sync/RLS, ambiguous product calls | Strong model on the lead |

Keep the lead session on one model; dispatch easy work rather than mid-task model switching (cache does not transfer cleanly).

## Guardrails

- Thin UI; domain rules stay in core (mobile) or Postgres invariants (web) — not a parallel TS domain
- iOS runtime/Simulator is out of scope until #49 is decided
- No PilcrowMD-style NOW/inbox/facts control plane
- Do not open a PR or merge unless the user asked for that step
- Do not reorder delivery (core → platforms → tests) when adding skills
