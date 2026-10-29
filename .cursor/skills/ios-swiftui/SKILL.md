---
name: ios-swiftui
description: >-
  Implement GymManager iOS client as SwiftUI over KMP framework: thin UI,
  build-spec and wiring docs first; no claimed Simulator green builds until
  Mac access is decided. Use when editing apps/ios/, SwiftUI stubs, XCFramework
  wiring docs, or the user says iOS / SwiftUI.
---

# iOS (SwiftUI) — build-spec first

Own `apps/ios/`: SwiftUI presentation over the KMP framework exported from `core/`. Native UI: [ADR 0003](../../../docs/decisions/0003-native-ui-per-platform.md). Client role: [clients](../../../docs/architecture/clients.md). Scaffold step C: [phase-1 playbook](../../../docs/plans/phase-1-scaffold-playbook.md).

**Runtime mode today:** docs + project structure + framework-wiring instructions. Do **not** claim a green Xcode/Simulator build from a Windows agent. Full runtime/Simulator scope waits on [#49](https://github.com/MarekBiolik/GymManager/issues/49) (Mac access decision).

Env: [references/env-checklist.md](references/env-checklist.md).

**Theme:** consume generated **SwiftUI token structs** / Color extensions. Do **not** hardcode palette hex — change values via SSOT + regenerate (`design-tokens` skill). Build-spec first until Mac access ([#49](https://github.com/MarekBiolik/GymManager/issues/49)). See [references/theming.md](references/theming.md).

## Workflow

Copy and track:

```
iOS slice:
- [ ] 1. Confirm core framework contract
- [ ] 2. Thin SwiftUI (or stubs)
- [ ] 3. Wiring docs
- [ ] 4. Verify within mode
- [ ] 5. Hand-off
```

### 1. Confirm core framework contract

iOS consumes the public core API via XCFramework / local SPM from KMP (same commit as the app). Domain rules stay in `core-kmp`. If the slice needs new shared behavior, land core first (`platform-delivery`).

**Done when:** known framework API / packaging approach for this slice (or linked core change).

### 2. Thin SwiftUI

- SwiftUI views without business logic; map at the edge from core types
- Navigation, Keychain (if tokens outside SDK), system permissions — iOS-owned
- Colors / spacing / type from **generated** token structs — never hardcode brand hex in feature views
- When shipping Google Sign-In: App Store also requires **Sign in with Apple** — see [auth](../../../docs/architecture/auth.md)

On Windows / no Mac: prefer stubs, placeholders, and accurate structure over inventing unverifiable runtime code paths.

**Done when:** UI layer stays thin; no parallel Swift domain.

### 3. Wiring docs

Keep `apps/ios/README.md` (or linked docs) executable on a Mac: how to build/link the framework from Gradle KMP, open the Xcode project, run on Simulator **when** a Mac is available.

Do not treat Dashboard or ad-hoc binary drops as source of truth — framework comes from the monorepo `core` at the same commit ([ADR 0005](../../../docs/decisions/0005-monorepo-tooling.md)).

**Done when:** a Mac user could follow the docs without tribal knowledge.

### 4. Verify within mode

| Mode | Success looks like |
|------|-------------------|
| **Build-spec (default now)** | Xcode project + wiring docs consistent; no false “Simulator passed” claims |
| **Runtime (after #49)** | `xcodebuild` / Simulator smoke on Mac or agreed cloud macOS runner |

CI need not green iOS on Linux day one ([playbook](../../../docs/plans/phase-1-scaffold-playbook.md) step C/F).

**Done when:** verification matches the active mode above.

### 5. Hand-off

Short status: files/docs touched, mode (build-spec vs runtime), blockers on #49 if any. PR/merge only if the user asked.

**Done when:** iOS AC for the slice are tickable under build-spec constraints.

## Guardrails

- Thin SwiftUI over KMP — no business rules in Views
- No claimed Simulator/Xcode success without Mac access (#49)
- Sign in with Apple when Google Sign-In ships on iOS
- No parallel palette — tokens via `design-tokens` SSOT + regenerate (appearance SSOT not owned here)
- Prefer strong model for interop/auth edges; cheap helpers for search/doc edits
