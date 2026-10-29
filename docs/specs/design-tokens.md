# Spec: design tokens contract and generation pipeline

**Status:** SPEC ONLY — no generator implementation, no app theme wiring in this ticket.  
**Issue:** [#61](https://github.com/MarekBiolik/GymManager/issues/61)  
**Parent decision:** [ADR 0010](../decisions/0010-design-tokens-ssot.md)  
**First style values:** [visual-style-fitness-gym](visual-style-fitness-gym.md) ([#62](https://github.com/MarekBiolik/GymManager/issues/62))

This document defines the **token contract** and **generation pipeline** so scaffold and later CI can implement without re-deciding architecture. UI remains native per platform ([ADR 0003](../decisions/0003-native-ui-per-platform.md)); only values and names are shared.

## Goals

1. Three-layer token model: primitive → semantic → component.
2. Stable naming so generators and platforms map 1:1.
3. Source layout under `design/tokens/` (DTCG-oriented JSON).
4. Explicit generator inputs/outputs for Compose, SwiftUI, and web (CSS + TypeScript).
5. Dark/light via **semantic override**; preferred mode dark OLED.
6. Swappability and CI regenerate-diff-clean gate.

## Non-goals

| Out of scope | Where |
|--------------|--------|
| Generator / Style Dictionary implementation code | scaffold / follow-up after this spec |
| `apps/*` theme wiring | [#14](https://github.com/MarekBiolik/GymManager/issues/14) and platform skills |
| Locking a second visual style or light palette | [OQ-09](../open-questions.md) (when present) |
| Shared UI components across platforms | ADR 0003 — not tokens |

## Three-layer model

| Layer | Role | Example |
|-------|------|---------|
| **Primitive** | Raw palette, scales, durations — no UI meaning | `color.orange.500`, `space.4`, `duration.fast` |
| **Semantic** | Role-based aliases → primitives; **theme mode lives here** | `color.bg.canvas`, `color.action.primary`, `color.text.primary` |
| **Component** | Optional tight aliases for recurring UI patterns | `button.primary.bg`, `card.surface` |

Rules:

- UI and platform theme code consume **semantic** (and component) tokens — not raw primitives — except rare documented exceptions (e.g. charts needing a series palette).
- **Dark / light** = alternate semantic maps over the same primitives (or a light primitive subset). Preferred default: **dark OLED**. Light mode deferred to governance (OQ-09).
- Style swap = edit primitives and/or semantic maps; regenerate; do **not** rewrite screens.

## Naming convention

Use a predictable path that maps to CSS custom properties and platform identifiers:

```text
--{category}-{item}-{variant?}-{state?}
```

| Segment | Meaning | Examples |
|---------|---------|----------|
| `category` | Domain of the token | `color`, `space`, `radius`, `type`, `elevation`, `motion` |
| `item` | Role or scale step | `bg`, `text`, `action`, `4`, `md`, `fast` |
| `variant` | Optional qualifier | `primary`, `canvas`, `muted`, `brand` |
| `state` | Optional interaction state | `hover`, `pressed`, `disabled`, `focus` |

Examples:

- `--color-bg-canvas`
- `--color-action-primary`
- `--color-action-primary-pressed`
- `--space-4`
- `--type-body-size`
- `--motion-duration-fast`

JSON / DTCG paths use the same segments as nested keys, e.g. `color.bg.canvas`. Generators flatten to platform idioms (`ColorBgCanvas`, `colorBgCanvas`, `--color-bg-canvas`).

## Source layout (`design/tokens/`)

W3C Design Tokens Community Group (DTCG)–oriented JSON (or JSONC if comments are needed locally — prefer plain JSON in CI).

```text
design/
  tokens/
    primitive/
      color.json          # palette ramps
      space.json          # spacing scale (4px base)
      radius.json
      typography.json
      elevation.json
      motion.json
    semantic/
      color.dark.json     # preferred OLED dark map (default)
      color.light.json    # deferred / stub until OQ-09
      typography.json
      space.json          # if semantic aliases needed
    component/            # optional; may start empty
      button.json
      card.json
    $metadata.json        # optional: active theme id, version
  # Generator (implementation later — not this SPEC):
  # package.json, scripts/generate.mjs, …
```

Token file shape (illustrative):

```json
{
  "color": {
    "orange": {
      "500": { "$type": "color", "$value": "#FF6B35" }
    }
  }
}
```

Semantic reference (illustrative):

```json
{
  "color": {
    "action": {
      "primary": {
        "$type": "color",
        "$value": "{color.orange.500}"
      }
    }
  }
}
```

Exact first-style hex and scales: [visual-style-fitness-gym](visual-style-fitness-gym.md).

## Generator — inputs and outputs

**Input:** all files under `design/tokens/` (resolved references).  
**Tooling (ADR 0010):** thin custom Node generator under `design/` first; Style Dictionary optional later.

| Platform | Output artifact (target paths — scaffold may adjust) | Contents |
|----------|------------------------------------------------------|----------|
| Android | e.g. `apps/android/.../theme/GeneratedTokens.kt` (+ `Theme.kt` wiring that **reads** generated values) | Compose `Color` / `Typography` / spacing Dp from semantic tokens |
| iOS | e.g. `apps/ios/.../Theme/GeneratedTokens.swift` | SwiftUI `Color`, font sizes, spacing CGFloat |
| Web | e.g. `apps/web/src/styles/tokens.css` + `apps/web/src/theme/tokens.ts` | CSS custom properties + TS const map |

Generation rules:

- Resolve `{reference}` aliases before emit.
- Emit **semantic** (and component) tokens into public theme APIs; primitives may be emitted for debugging but must not be the primary UI API.
- Active color mode default = dark semantic map.
- Do not invent platform widgets in the generator — values only.

## Dark / light theming

1. Primitives stay mode-agnostic where possible (shared ramps).
2. Semantic `color.*.json` selects the active surface/text/action mapping.
3. Preferred shipped mode: **dark OLED** (near-black canvases).
4. Light semantic file may exist as a stub; enabling it is a product decision under OQ-09 — not required for MVP UI.

## Swappability rule

- Change **primitives** and/or **semantics** (and optional component aliases) to restyle.
- **UI must not hardcode palette hex**, raw type sizes, or spacing magic numbers that duplicate tokens.
- Platform code may use Material / HIG structure but must bind colors/spacing/type from generated tokens.

## CI gate — regenerate-diff-clean

When the generator exists (scaffold / follow-up):

1. CI runs the generator (same command as local `pnpm`/`npm` script under `design/`).
2. `git status` / `git diff` on generated output paths must be **empty**.
3. Failure means committed outputs drifted from SSOT — fix by regenerating and committing, or fix the generator.

Until the generator ships, this gate is **documented expectation only** (no failing job yet).

## Scaffold note ([#14](https://github.com/MarekBiolik/GymManager/issues/14))

Monorepo scaffold **consumes** this contract: create `design/tokens/` stubs, generator entrypoint, and wire apps to generated paths. This SPEC does not implement those files.

## Related

- [ADR 0010](../decisions/0010-design-tokens-ssot.md)
- [ADR 0003](../decisions/0003-native-ui-per-platform.md)
- [visual-style-fitness-gym](visual-style-fitness-gym.md)
- [roadmap](../roadmap.md) Phase 1 foundations
