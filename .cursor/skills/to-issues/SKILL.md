---
name: to-issues
description: >-
  Break a plan or spec into GitHub epic and task issues with AC checkboxes,
  per-area tasks, blockers, and one-issue-one-PR sizing. Use when slicing a
  plan into issues, after plan mode agrees a multi-slice feature, before
  platform-delivery coding, or when the user says to-issues / epic / tasks /
  re-slice.
---

# To-issues

Turn an agreed plan or spec into a **GitHub issue tree**: phase/feature **epics** and implementable **tasks**. Each task is one focused PR. English titles/bodies. AC checkboxes = the agent’s done-list ([workflow](../../rules/workflow-issue-pr-commit.mdc)).

## Workflow

Copy and track:

```
To-issues:
- [ ] 1. Inventory slices
- [ ] 2. Size each slice (epic vs task)
- [ ] 3. Wire blockers
- [ ] 4. Create / update issues via gh
- [ ] 5. Hand off
```

### 1. Inventory slices

Read the plan/spec. List feature areas and which surfaces each touches: `supabase/` (schema), `core/`, `apps/android/`, `apps/web/`, docs-only, etc.

**Done when:** a flat list of feature names exists (e.g. Settings, Foods, Diary).

### 2. Size each slice (epic vs task)

| Kind | What it is | Ships code? |
|------|------------|-------------|
| **Epic** | Phase or feature umbrella | No — checklist of child tasks + module AC |
| **Task** | One **area** of one feature | Yes — one focused PR |

**Per-area tasks** (default for multi-platform features — phase-2 pattern):

| Task area | Typical contents | Tests in the same PR |
|-----------|------------------|----------------------|
| `schema+core` | Migration/RLS + KMP domain/repos (+ thin web API mirror only if required for contract) | `commonTest` / migration checks |
| `android` | Compose UI over the merged core seam | Required emulator smoke (`android-emulator` / `qa-testing`) |
| `web` | React + supabase-js UI | `pnpm` build + browser smoke of the changed flow |
| `docs` | ADR/spec only | N/A (review) |

**Size budget (split when any is true):**

- The work spans **core + two UI platforms** in one issue
- Or the draft PR would mix unrelated features
- Or a single task would need both a large schema change and full UI polish

Then: epic for the feature + one task per area (not one mega-issue).

**One issue = one focused PR.** Epics do not get implementation PRs; tasks do.

**Done when:** every feature is either a docs-only task or an epic with named per-area tasks.

### 3. Wire blockers

Default dependency for a feature that needs shared contract:

```text
schema+core  --blocks-->  android
schema+core  --blocks-->  web
android  ||  web   (parallel after core merges)
```

Special cases:

- Docs/ADR foundation may block the first `schema+core` of the phase
- OFF search (no schema) may block barcode (reuses import)
Encode blockers in each task body as `Depends on #N` (and list child tasks on the epic). Prefer GitHub native parent/sub-issue or blocking links when the API allows; body lines are the minimum.

**Done when:** every UI task names its core (or other) dependency; parallel pairs are explicit.

### 4. Create / update issues via gh

**Titles (English):**

| Kind | Shape | Example |
|------|-------|---------|
| Phase epic | `Phase N: <name> (tracking)` | `Phase 3: Nutrition module (tracking)` |
| Feature epic | `epic: <feature> (Android + web)` | `epic: nutrition settings + meal types` |
| Task | `feat(<area>): <subject>` | `feat(core): nutrition_settings + meal_types repos` |

Areas in titles: `core`, `android`, `web`, `docs`, or `db` when schema-only.

**Bodies:**

- Epic: goal, child task checklist (`- [ ] #N …`), module-level AC, skills pointer
- Task: what to build, `Depends on #N` if any, AC checkboxes (implementable), test plan line, skills to load, optional `Reference: branch backup/…` when porting

**Labels:** `ready-for-agent` on tasks ready to implement; `enhancement` / `documentation` as appropriate; epics may stay without `ready-for-agent` until children exist.

**gh on Windows (PowerShell):** write body to a temp file; use `--body-file`. Retry create on GraphQL flakes (sleep 2–3s). Do **not** rely on bash heredocs in PowerShell.

**Done when:** `gh issue list` shows the epic tree; every task has AC checkboxes; blockers appear in bodies.

### 5. Hand off

Comment on the phase epic with the tree summary (or paste into the epic body). Point `platform-delivery` at the **next unblocked task** (usually the first `schema+core`), not at the whole epic.

**Done when:** user can open one task and run `platform-delivery` without re-slicing.

## Anti-patterns (fix by splitting)

| Smell | Fix |
|-------|-----|
| One issue AC lists migration + Android + web | Split into per-area tasks under a feature epic |
| One PR implements three features | Split into feature epics, then tasks |
| UI task has no `Depends on` core | Add blocker; do not start UI before core PR merges (unless user stacks explicitly) |
| Epic opened as `ready-for-agent` for coding | Keep coding labels on **tasks** only |

## Related

- [platform-delivery](../platform-delivery/SKILL.md) — implements one task at a time (core PR, then parallel UI PRs)
- [workflow-issue-pr-commit](../../rules/workflow-issue-pr-commit.mdc) — issue → PR → commit
- [agent-dev-environment](../../../docs/conventions/agent-dev-environment.md) — delivery flow overview
