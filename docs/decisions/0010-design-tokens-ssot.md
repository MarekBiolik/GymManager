# ADR 0010 — Design tokens SSOT and cross-platform generation

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ design-token foundation planning)

## Context

GymManager ships three native UIs ([ADR 0003](0003-native-ui-per-platform.md)): Jetpack Compose, SwiftUI, and React. Palette, type, spacing, and motion must stay coherent across platforms without rewriting screens when the visual style swaps. Forces: solo maintenance, tokens-first before scaffold ([#14](https://github.com/MarekBiolik/GymManager/issues/14)), no override of native UI ownership, and a clear regenerate-clean CI gate so generated theme artifacts cannot drift from the source of truth.

## Decision

1. **Single source of truth (SSOT):** language-neutral design-token files live under `design/tokens/` (W3C Design Tokens / DTCG-oriented JSON or equivalent). Agents and humans edit tokens here — not hex/literals in app UI.
2. **Generator → per-platform outputs:** a thin Node generator under `design/` reads the SSOT and emits native theme artifacts:
   - Android: Compose theme / token Kotlin (e.g. `Theme.kt` / color/type/spacing objects)
   - iOS: SwiftUI token structs / Color+Font extensions
   - Web: CSS custom properties + TypeScript token module
3. **UI stays platform-native.** This ADR shares **token values and naming** only. It does **not** override [ADR 0003](0003-native-ui-per-platform.md): screens remain Compose / SwiftUI / React; there is no shared UI toolkit.
4. **Style swaps** are token edits (primitive and/or semantic layers), not UI rewrites. Preferred first style and dark OLED defaults are specified separately ([visual style](../specs/visual-style-fitness-gym.md) when present).
5. **Tooling:** prefer a **thin custom Node generator** for the first pipeline (explicit, easy to read in CI). **Style Dictionary** (or similar) remains optional later if token graph complexity warrants it — not required to start.

## Alternatives considered

1. **Manual per-platform mirror** — each app keeps its own theme constants; copy hex by hand. Pros: no generator. Cons: drift is inevitable; style swap = three edits and review load; rejected.
2. **KMP-shared tokens (+ web mirror)** — put token constants in `core` for Android/iOS and mirror for web. Pros: one Kotlin source for mobile. Cons: pulls presentation concerns into domain core; web still needs a parallel path; fights ADR 0006 web boundary; rejected for tokens.
3. **SSOT files + generator → native outputs** — language-neutral tokens, generate Compose / SwiftUI / CSS+TS. Pros: one edit surface, CI can prove regen-clean, UI stays native. **Chosen.**

## Consequences

### Positive

- **Swappability:** change primitives/semantics in `design/tokens/`, regenerate, ship a new look without rewriting screens.
- **CI regen-clean:** regenerate outputs in CI; working tree must stay clean — committed generated files cannot silently diverge from SSOT.
- **Native UI preserved:** platforms keep Compose / SwiftUI / React idioms and Material/HIG where appropriate; only values are shared.

### Negative / costs

- Generator and mapping docs are another surface to maintain until scaffold lands.
- Agents must not hardcode palette values in UI; discipline enforced by skills and review.
- Light-mode and multi-style governance stay open until theming OQ closes (see open-questions follow-up).

### What to refine later

- Concrete token contract and pipeline: [design-tokens spec](../specs/design-tokens.md) ([#61](https://github.com/MarekBiolik/GymManager/issues/61)).
- First visual style values: [Fitness/Gym visual style](../specs/visual-style-fitness-gym.md) ([#62](https://github.com/MarekBiolik/GymManager/issues/62)).
- Roadmap / ADR 0003 / open-questions alignment: [#65](https://github.com/MarekBiolik/GymManager/issues/65).
- Scaffold ([#14](https://github.com/MarekBiolik/GymManager/issues/14)) consumes generated outputs under `apps/*` — no theme wiring in this ADR alone.

## Related

- Does **not** override [ADR 0003](0003-native-ui-per-platform.md) (native UI ×3)
- [roadmap](../roadmap.md) — Phase 1 foundations mention tokens contract (aligned in docs follow-up)
- [open-questions](../open-questions.md) — theming / style-swap governance (OQ-09 when added)
- [docs index](../README.md)
- Follow-up specs: [#61](https://github.com/MarekBiolik/GymManager/issues/61), [#62](https://github.com/MarekBiolik/GymManager/issues/62)
