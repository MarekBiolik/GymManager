# QA / testing environment checklist

QA reuses area toolchains — install what the change set needs.

| Layer | Needs |
|-------|--------|
| Core | JDK, Gradle wrapper (`core-kmp` env) |
| Supabase/RLS | Supabase CLI, Docker for local (`supabase-db` env) |
| Web | Node LTS, pnpm (`web-react` env) |
| Android smoke | SDK, adb, AVD/device (`android-emulator` env) |
| iOS runtime | Deferred — [#49](https://github.com/MarekBiolik/GymManager/issues/49) |

## Related skills

- `platform-delivery` — skip gate + when to run QA
- `tdd` — authoring tests with the change
- `code-review` — standards + spec review (not a test runner)
