# ADR 0001 — Shared core: Kotlin Multiplatform

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ architecture planning session)

## Context

The app must run on Android, iOS, and web. We want professional maintenance: domain logic (nutrition, training, measurements, sync) must not be copied across three UIs. Preferred model: **shared core + native UI per platform** (not one cross-platform UI).

## Decision

We build a shared **`core` module in Kotlin Multiplatform (KMP)** with domain rules, use cases, and data access (Supabase). Android and iOS consume core natively; on day 1 web does **not** link a KMP binary — `supabase-js` + thin TS ([ADR 0006](0006-web-supabase-js-without-kmp-js.md), closes OQ-01).

## Alternatives considered

1. **Expo / React Native (+ web)** — one UI for all platforms, strong TypeScript DX. Rejected: less native UI; inconsistent with the chosen “core + native UI” model.
2. **Flutter** — one UI, mature multiplatform. Rejected: Dart instead of Kotlin/Swift/TS; abandons native UI per platform.
3. **Backend-only “core” (thin clients without a shared lib)** — simple, but offline rules and shared validation return to duplication or tie UX to the network.
4. **Kotlin Multiplatform** — chosen: one core language close to Android, official iOS support, JS/Wasm path for web (with risk).

## Consequences

- Positive: one rules implementation; unit tests in core; Android integrates cheapest.
- Negative: must maintain the KMP toolchain + Swift bindings; web without a shared binary needs domain discipline (ADR 0006).
- Later: scaffold `core/`, public use-case APIs; revisit KMP-JS only per ADR 0006 criteria.

## Related

- [shared-core](../architecture/shared-core.md)
- [ADR 0003](0003-native-ui-per-platform.md)
- [ADR 0006](0006-web-supabase-js-without-kmp-js.md) (OQ-01)
