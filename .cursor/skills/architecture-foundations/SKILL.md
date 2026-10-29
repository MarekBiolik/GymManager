---
name: architecture-foundations
description: Bootstraps high-level architecture documentation for a new repo or app restart—vision, stack ADRs, domain outline, conventions, open questions—before scaffolding code. Use only when explicitly invoked (e.g. /architecture-foundations).
disable-model-invocation: true
---

# Architecture foundations

Produce a **docs-first** foundation for a new repo or clean restart. Output is documentation under `docs/` (or path the user names). **No application scaffolding** unless the user explicitly expands scope.

Also **seed project steering rules** under `.cursor/rules/` before writing docs, so English public artifacts and commit/PR discipline apply from commit #1.

## Workflow

Copy and track:

```
Foundations progress:
- [ ] 1. Survey
- [ ] 2. Critical decisions (interview)
- [ ] 3. Plan (docs only)
- [ ] 4. Seed project steering
- [ ] 5. Write docs + ADRs
- [ ] 6. Hand-off
```

### 1. Survey

Read the workspace (readonly): git tracked files, existing `docs/`, leftover prototypes, empty dirs.

Record in the plan: **greenfield** vs **restart on top of debris**, and what to ignore vs clean later.

**Done when:** one short “state of repo” paragraph exists for the plan.

### 2. Critical decisions (interview)

Ask **1–2 questions at a time** (prefer structured options). Do not invent stack choices when the user is undecided—explain tradeoffs briefly, then ask again.

Decide at minimum:

| Decision | Typical forks |
|----------|----------------|
| Platforms | mobile OS(s), desktop native vs web-on-laptop |
| Client shape | one cross-platform UI vs shared core + native UI per platform |
| Backend / data / auth | BaaS vs custom; relational vs document; how sync + minimum PII |

When the user lacks vocabulary, define terms **in product context** (e.g. lock-in, aggregation) before asking them to choose.

If a choice is high-cost (e.g. native UI ×3), state the maintenance consequence once, then accept their call.

**Done when:** platforms, client shape, and backend are chosen (or explicitly deferred into open questions with a default lean).

### 3. Plan (docs only)

Create a plan that lists exact files to write. Default tree: see [docs-tree.md](docs-tree.md).

Plan must include:

- Target architecture sketch (mermaid OK)
- ADR list for **accepted** decisions only
- Open-questions list for deferred areas
- Explicit **out of scope**: app code, product AI agents/skills/workflows (unless user insists)
- Note that step 4 seeds `.cursor/rules/` before docs

**Done when:** user confirms the plan.

### 4. Seed project steering

Write the always-on project rules from [project-rules.md](project-rules.md) into `.cursor/rules/`:

- `commit-after-steps.mdc`
- `english-public-artifacts.mdc`
- `workflow-issue-pr-commit.mdc`
- `docs-current-state.mdc`

If a file already exists: **reconcile** (keep stronger/clearer wording; do not overwrite silently). Note any drift in the hand-off.

This step is the **first write** into the repo for a greenfield kickoff—before any `docs/` content—so English-public and commit rules bind the foundations docs themselves.

**Done when:** all seeded rule files exist under `.cursor/rules/` before the first docs commit.

### 5. Write docs + ADRs

Implement the plan. Mark todos as you go. All repo docs and commits are **English** (enforced by the seeded `english-public-artifacts` rule).

Rules:

- Docs are the source of truth; code comes in a later phase.
- Every accepted stack/structure choice → ADR (`accepted` + date + alternatives).
- Every deferred topic → `docs/open-questions.md` with id `OQ-NN`, status, close-by phase.
- Domain model = **outline** (entities + glossary), not final SQL.
- Conventions = thin and actionable (git, coding seams, testing priority, CI goals, docs lifecycle).
- Docs = **current state** only (`docs-current-state`); no prototype post-mortems. Clean working-tree debris when asked — do not immortalize cleanup in `docs/`.

**Done when:** every planned file exists; index (`docs/README.md`) links them; ADRs and OQs are consistent with the interview.

### 6. Hand-off

Tell the user:

1. Where to start reading (`docs/README.md`)
2. Which decisions are locked (ADR ids)
3. Which OQs block the next phase (usually scaffolding)
4. That ongoing work uses **issue → PR → focused commits → merge** (seeded `workflow-issue-pr-commit`; skills `to-issues` and `pr`)
5. Propose **next phase** (typically monorepo/scaffold or slicing open questions into issues)—do not start it unless asked
6. Mentions commits/push/merge only per the seeded commit and workflow rules (or when the user asks)

**Done when:** hand-off message sent; foundations todos completed.

## Non-goals

- Implementing `core/`, `apps/*`, or infra beyond docs
- Designing the **product’s** AI agent workflows (dev process rules seeded here are fine)
- Final UI/UX or pixel design
- Closing every OQ in foundations—defer is success

## Quality bar

- Concise **English** for all docs and commits from commit #1—because step 4 seeds `english-public-artifacts` before writing docs (user chat may stay Polish)
- Prefer pointed docs over essays; diagrams where they replace paragraphs
- No fake version pins; pin versions at scaffolding time
- Restart/legacy: treat missing sources as greenfield; document debris explicitly
