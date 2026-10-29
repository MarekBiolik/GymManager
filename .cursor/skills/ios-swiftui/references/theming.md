# iOS theming from design tokens

Pipeline owner: `design-tokens` skill. Contract: [design-tokens.md](../../../../docs/specs/design-tokens.md).

## Wiring

- Generated `GeneratedTokens.swift` (path set at scaffold) exposes Color, spacing, and type sizes.
- App `Theme` / environment values read those constants — SwiftUI stays native ([ADR 0003](../../../../docs/decisions/0003-native-ui-per-platform.md)).
- Preferred mode: dark OLED until OQ-09.

## Mode

Until Mac access ([#49](https://github.com/MarekBiolik/GymManager/issues/49)): document paths and stubs accurately; do not claim Simulator verification of theme rendering from Windows.

## Do not

- Hardcode `Color(red:…)` brand values in feature views
- Hand-edit generated token files — regenerate from `design/tokens/`
