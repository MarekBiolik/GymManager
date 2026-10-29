# Android theming from design tokens

Pipeline owner: `design-tokens` skill. Contract: [design-tokens.md](../../../../docs/specs/design-tokens.md). Style: [visual-style-fitness-gym.md](../../../../docs/specs/visual-style-fitness-gym.md).

## Wiring

1. Generated Kotlin token objects land under the Android theme package (path set at scaffold).
2. `Theme.kt` / `GymTheme` maps semantic tokens into `ColorScheme`, typography, and spacing — Material 3 structure is fine; **values** come from generated tokens.
3. Preferred mode: dark OLED semantic map until OQ-09 decides otherwise.

## Consumption

```kotlin
// Prefer theme accessors — not Color(0xFFFF6B35) in feature UI
MaterialTheme.colorScheme.primary
GymTheme.spaces.md  // if exposed
```

## Do not

- Hardcode brand hex in composables or XML
- Hand-edit generated token files — regenerate from `design/tokens/`
- Invent a second Android-only palette that drifts from SSOT
