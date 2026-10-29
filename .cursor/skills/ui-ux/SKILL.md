---
name: ui-ux
description: >-
  Shape GymManager cross-platform UX contracts: flows, layout intent,
  accessibility, consistency — appearance values come from design-tokens SSOT
  and the Fitness/Gym visual style. Use when designing or reviewing
  interaction/UX across clients, writing UX specs, a11y checks, or the user
  says UX / UI design / flows.
---

# UI / UX

Own **cross-platform UX intent**: flows, information architecture, layout purpose, accessibility expectations, consistency notes. Do **not** own domain rules (→ `core-kmp` / `supabase-db`), platform implementation (→ `android-compose`, `web-react`, `ios-swiftui`), or the token **pipeline** (→ `design-tokens`).

**Appearance SSOT:** [design-tokens](../design-tokens/SKILL.md) skill + [Fitness/Gym visual style](../../../docs/specs/visual-style-fitness-gym.md) + [ADR 0010](../../../docs/decisions/0010-design-tokens-ssot.md). UX proposes layout/flow; **palette, type, space, motion values** change via tokens — not by hardcoding hex in specs or apps.

Native UI per platform: [ADR 0003](../../../docs/decisions/0003-native-ui-per-platform.md). Module-switching navigation ([OQ-08](../../../docs/open-questions.md)) stays **open** until phase 6 — do not close it casually in a random screen PR. Light mode / style-swap policy: [OQ-09](../../../docs/open-questions.md).

Deep topics: [references/flows.md](references/flows.md), [references/a11y-floor.md](references/a11y-floor.md). Env: [references/env-checklist.md](references/env-checklist.md).

## Scope split

| This skill | Not this skill |
|------------|----------------|
| Flow / wire intent, screen jobs, empty/error states | Compose / SwiftUI / React implementation |
| A11y expectations (contrast, focus, labels, motion) | Editing `design/tokens/` or the generator → `design-tokens` |
| Consistency of interaction across clients | Parallel domain or i18n framework (UI copy later; no i18n system now) |
| Specs, prototypes, review notes | Emulator/device smoke → android-emulator / QA |

Honour the Fitness/Gym block-based layout intent and project frontend design guidance. Prefer intentional brand and composition — **use the token SSOT**; do not invent a second palette or silently lock branding outside tokens.

## Workflow

Copy and track:

```
UX slice:
- [ ] 1. Clarify surface and constraints
- [ ] 2. Contract or prototype
- [ ] 3. A11y floor
- [ ] 4. Hand off to platforms
```

### 1. Clarify surface and constraints

Which clients? Online-first MVP assumptions? Auth/PII constraints from [auth](../../../docs/architecture/auth.md)? If module switching is in scope, treat OQ-08 as unresolved unless the user explicitly decides it. Appearance constraints come from the visual style + tokens — not a blank slate.

Use `interview` when forks remain; `research` for platform HIG/a11y facts when needed.

**Done when:** target platforms and open product forks are listed (including OQ-08 if relevant).

### 2. Contract or prototype

Prefer a short UX contract (flow steps, one job per screen/section, primary actions, empty/error/loading). Use `prototype` when a throwaway probe answers a feel/structure question faster than prose.

Reference semantic token **roles** (`color.action.primary`, block layout) rather than embedding hex as the long-term contract. Do not implement three native UIs here.

**Done when:** platforms can implement without guessing the flow’s purpose or inventing a private palette.

### 3. A11y floor

See [references/a11y-floor.md](references/a11y-floor.md). Minimum:

- Meaningful labels for icon-only controls
- Body text contrast ≥ 4.5:1; do not rely on color alone
- Keyboard / focus order sensible on web; large enough targets on mobile
- Respect reduced-motion preferences when motion is proposed
- Errors tell the user what happened and what to do next

**Done when:** the contract calls out a11y-relevant states (or explicitly defers a named item).

### 4. Hand off to platforms

Point implementers at `android-compose` / `web-react` / `ios-swiftui` and `design-tokens` for value changes. Keep a thin status note: contract location, open questions (OQ-08/OQ-09), deferred polish.

PR/merge only if the user asked. Tick UX issue AC when this slice’s UX work is done.

**Done when:** platform agents have a clear contract; no silent branding lock-in outside the token SSOT.

## Guardrails

- No silent branding lock-in — use token SSOT / do not hardcode palette
- Appearance values → `design-tokens`; flows/a11y → this skill
- No domain logic in UX specs
- No premature i18n architecture
- Do not close OQ-08 without an explicit decision
- Prefer strong model for product UX judgement; cheap helpers for research lookups
