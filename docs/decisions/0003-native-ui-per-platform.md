# ADR 0003 — Native UI per platform

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ architecture planning session)

## Context

Platform goal: Android + iOS + web. The product owner prefers “shared core, and each platform implements what it must” — maximally native UI (SwiftUI, Jetpack Compose, React on web), instead of one cross-platform UI framework.

Conscious trade-off: **maintaining three UIs** for a moderately complex app costs more than Expo/Flutter, but matches the chosen philosophy and native quality.

## Decision

Each platform has its **own UI layer**:

- Android → Jetpack Compose (Kotlin)
- iOS → SwiftUI (Swift)
- Web → React + TypeScript

Business logic is not reimplemented in UI — it goes to core ([ADR 0001](0001-shared-core-kmp.md)).

## Alternatives considered

1. **One cross-platform UI (Expo RN / Flutter)** — less UI work, faster feature parity. Rejected deliberately for nativeness and the core+native model.
2. **Web + PWA only** — cheapest, no App Store. Rejected as the only form: we want native mobile apps.
3. **Native UI per platform** — chosen despite triple UI cost.

## Consequences

- Positive: full access to platform APIs and look-and-feel; clear ownership split with core.
- Negative: every screen/UI feature ×3; longer time-to-feature; need strong “logic only in core” discipline.
- Later: OQ-08 (module navigation); scaffold `apps/*`.
- **Shared visual tokens (not shared UI):** [ADR 0010](0010-design-tokens-ssot.md) adds a language-neutral token SSOT + generator → Compose / SwiftUI / CSS+TS outputs. Native UI ownership in this ADR is **unchanged** — only token values and naming are shared.

## Related

- [clients](../architecture/clients.md)
- [ADR 0001](0001-shared-core-kmp.md)
- [ADR 0010](0010-design-tokens-ssot.md) — design tokens SSOT (does not override this ADR)
- [vision](../00-vision.md)
