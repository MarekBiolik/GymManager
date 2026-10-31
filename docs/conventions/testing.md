# Testing

## Priority

1. **Unit tests in `core`** — domain rules, validation, use cases (highest ROI).
2. **Migration / RLS tests** — policies reject other users’ rows (SQL or Supabase integration tests).
3. **UI tests** — selectively on critical flows (login, save entry); not every screen at the start.

## Rules

- New business rule in core = test in the same PR.
- Tests do not call production Supabase; use local / mocks / staging project.
- Flaky test = bug; do not leave permanent `skip` without an issue.

## Per layer (tooling direction)

| Layer | Direction |
|-------|-----------|
| core (KMP) | `kotlin.test` / JUnit in commonTest |
| Android UI | Compose testing — later |
| iOS UI | XCTest / snapshot — later |
| Web | Vitest + Testing Library — later |
| RLS | Tests with two user JWTs |

## Coverage

No hard % target at the start. Require tests for: auth session handling, sync conflict helpers (when they exist), domain macro/aggregation calculations.

## Agent smoke (required on UI slices)

- **Android:** run `android-emulator`. Empty `adb devices` → start AVD `Medium_Phone_API_37.0` (or the first listed AVD) and wait for boot. Do not treat an idle emulator as “no emulator”.
- **Web:** production build **and** browser smoke of the changed flow.
- If a required layer cannot run after a real attempt: **stop and ask**. No skip gate.

## Manual

Before store release: smoke on a real iOS + Android device + browser (login, CRUD one entity, logout).
