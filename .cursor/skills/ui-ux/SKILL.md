---
name: ui-ux
description: >-
  Shape GymManager cross-platform UX contracts: flows, layout intent,
  accessibility, consistency — not platform code and not a premature design
  system. Use when designing or reviewing interaction/UX across clients,
  writing UX specs, a11y checks, or the user says UX / UI design / flows.
---

# UI / UX

Own **cross-platform UX intent**: flows, information architecture, layout purpose, accessibility expectations, consistency notes. Do **not** own domain rules (→ `core-kmp` / `supabase-db`) or platform implementation (→ `android-compose`, `web-react`, `ios-swiftui`).

Native UI per platform: [ADR 0003](../../../docs/decisions/0003-native-ui-per-platform.md). Module-switching navigation ([OQ-08](../../../docs/open-questions.md)) stays **open** until phase 6 — do not close it casually in a random screen PR.

## Scope split

| This skill | Not this skill |
|------------|----------------|
| Flow / wire intent, screen jobs, empty/error states | Compose / SwiftUI / React implementation details |
| A11y expectations (contrast, focus, labels, motion) | Inventing final branding as “done” before phase 6 |
| Consistency across Android / web / (later) iOS | Parallel domain or i18n framework (UI copy later; no i18n system now) |
| Specs, prototypes, review notes | Emulator/device smoke (→ android-emulator / QA) |

When visual craft starts, honour the project’s frontend design guidance (Cursor user rules / any future design-system doc). Prefer intentional brand and composition over generic “AI slop” layouts — without declaring a full design system complete early.

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

Which clients? Online-first MVP assumptions? Auth/PII constraints from [auth](../../../docs/architecture/auth.md)? If module switching is in scope, treat OQ-08 as unresolved unless the user explicitly decides it.

Use `interview` when forks remain; `research` for platform HIG/a11y facts when needed.

**Done when:** target platforms and open product forks are listed (including OQ-08 if relevant).

### 2. Contract or prototype

Prefer a short UX contract (flow steps, one job per screen/section, primary actions, empty/error/loading). Use `prototype` when a throwaway probe answers a feel/structure question faster than prose.

Do not implement three native UIs here — produce intent that platform skills can execute.

**Done when:** platforms can implement without guessing the flow’s purpose.

### 3. A11y floor

Minimum expectations for any UX you propose:

- Meaningful labels for icon-only controls
- Contrast suitable for body text; do not rely on color alone
- Keyboard / focus order sensible on web; large enough targets on mobile
- Respect reduced-motion preferences when motion is proposed
- Errors tell the user what happened and what to do next

**Done when:** the contract calls out a11y-relevant states (or explicitly defers a named item).

### 4. Hand off to platforms

Point implementers at `android-compose` / `web-react` / `ios-swiftui` (build-spec). Keep a thin status note: contract location, open questions, what is deferred to phase 6.

PR/merge only if the user asked. Tick UX issue AC when this slice’s UX work is done.

**Done when:** platform agents have a clear contract; no silent branding lock-in.

## Guardrails

- UX contracts ≠ design system “done”
- No domain logic in UX specs
- No premature i18n architecture
- Do not close OQ-08 without an explicit decision
- Prefer strong model for product UX judgement; cheap helpers for research lookups
