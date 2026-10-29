# iOS environment checklist

## Build-spec mode (default — Windows OK)

| Tool | Expectation |
|------|-------------|
| **Repo docs** | `apps/ios/` Xcode project tree + framework wiring README |
| **Gradle KMP** | `core` configured to export iOS framework (verified on Mac later) |
| **No local Xcode required** | Agents must not require Simulator to complete build-spec slices |

## Runtime mode (after [#49](https://github.com/MarekBiolik/GymManager/issues/49))

| Tool | Expectation |
|------|-------------|
| **macOS** | Physical Mac or agreed cloud macOS runner |
| **Xcode** | Version compatible with the project |
| **Apple Developer** | Needed for device/TestFlight/App Store; Simulator may work with free tooling for local smoke |
| **Signing** | Team/bundle ids configured outside git secrets |

## Typical commands (runtime mode only)

Follow `apps/ios/README.md` once present. Do not invent `xcodebuild` flags from memory when docs exist — prefer the README.

## Related

- Mac access / Simulator decision: [#49](https://github.com/MarekBiolik/GymManager/issues/49)
- Android (testable on Windows): `android-compose` + emulator #50
