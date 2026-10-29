# Platform outputs

Contract table: [docs/specs/design-tokens.md](../../../../docs/specs/design-tokens.md#generator--inputs-and-outputs). Paths may adjust at scaffold; names stay stable.

## Android (Compose)

- **Emit:** generated Kotlin color / type / spacing objects; `Theme.kt` (or MaterialTheme wrapper) **reads** those values.
- **Consume:** `MaterialTheme.colorScheme` / custom `GymTheme` extension properties bound to generated tokens.
- **Do not:** paste `#FF6B35` in feature composables.

## iOS (SwiftUI)

- **Emit:** `GeneratedTokens.swift` (or similar) with `Color`, spacing, font size constants.
- **Consume:** views use token struct / asset-free Color extensions from generated file.
- **Mode today:** build-spec first until Mac access ([#49](https://github.com/MarekBiolik/GymManager/issues/49)); still document token paths for scaffold.

## Web (React)

- **Emit:** `tokens.css` (CSS custom properties) + `tokens.ts` (typed map).
- **Consume:** CSS vars in stylesheets / Tailwind theme extension if used; TS for JS-driven values; thin theme hook reading vars or TS map.
- **Do not:** duplicate hex in component CSS modules as a second palette.

## CI

When generator exists: run generate → working tree diff on outputs must be clean. Hand-editing generated files is a bug.
