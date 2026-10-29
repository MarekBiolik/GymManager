# GymManager iOS (SwiftUI)

Native SwiftUI shell over the KMP `GymManagerCore` framework. **Build-spec / docs first** until Mac access is decided ([#49](https://github.com/MarekBiolik/GymManager/issues/49)). CI does not require a green Xcode build on Linux runners in phase 1.

## Layout

| Path | Role |
|------|------|
| `GymManager/` | SwiftUI app sources (Hello + theme) |
| `GymManager/Theme/GeneratedTokens.swift` | **Generated** from `design/tokens/` — do not hand-edit |
| `GymManager.xcodeproj/` | Xcode project |
| this README | Framework wiring from Gradle KMP |

## Build XCFramework / framework from Gradle (Mac)

From the repo root (requires JDK + Android SDK for the Android target of `:core`, and a Mac for iOS targets):

```bash
./gradlew :core:linkDebugFrameworkIosSimulatorArm64
# or assemble a fat XCFramework when you add an explicit Gradle task later
```

Framework base name: **`GymManagerCore`** (see `core/build.gradle.kts`).

### Wire into Xcode

1. Open `GymManager.xcodeproj` in Xcode.
2. App target → **General** → **Frameworks, Libraries, and Embedded Content** → add the built `GymManagerCore.framework` (or XCFramework).
3. Ensure **Framework Search Paths** includes the Gradle output directory, e.g.  
   `../../core/build/bin/iosSimulatorArm64/debugFramework`
4. Import in Swift: `import GymManagerCore`

Until the framework is linked, the Hello screen compiles against Swift-only stubs and generated design tokens.

## Tokens

Regenerate theme constants from the monorepo root:

```bash
pnpm tokens:generate
```

Prefer `GymTokens.*` over hardcoded colors ([ADR 0010](../../docs/decisions/0010-design-tokens-ssot.md)).

## Auth

Sign in with Apple lands with phase 2 (alongside Google). Not part of scaffold #14.
