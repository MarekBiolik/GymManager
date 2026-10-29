# ADR 0007 — Auth providers and minimum PII

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ OQ-02 closing session)

## Context

We need an account for **cross-device sync** and data isolation (RLS), without building a social profile. Forces: sign-in convenience, maintenance cost (Supabase Auth, [ADR 0002](0002-backend-supabase.md)), **minimum PII**, **App Store** requirements (Sign in with Apple when offering another third-party IdP — including Google).

Options from [OQ-02](../open-questions.md): Google OAuth, Sign in with Apple, magic link, anonymous account + later linking.

## Decision

1. **Providers:** **Google** (Android, iOS, web) and **Sign in with Apple** on **iOS** (required when offering Google on the App Store). On Android and web, Google alone is enough; Apple on web/Android is **not** in MVP.
2. **Session:** Supabase Auth (JWT + SDK refresh). One account = the same data on all clients.
3. **Minimum PII:** we do not collect name, gender, date of birth, phone, or address. Email lands in `auth.users` from the IdP (unavoidable with Google/Apple) — we do **not** copy it to `profiles`, do **not** use it for marketing. No person-identifying analytics at the start.
4. **`profiles`:** technical 1:1 record with `auth.users` — `id`, timestamps, app preferences (e.g. units). No display name / avatar from the IdP.
5. **Account deletion:** user initiates in settings; we delete app data (`user_id`) and the Auth record — outline in [auth](../architecture/auth.md#account-deletion).

**Beyond MVP:** magic link, email+password registration, anonymous accounts, linking multiple IdPs beyond Supabase defaults (e.g. same email Google↔Apple).

## Alternatives considered

1. **Google + Apple (iOS)** — chosen: low cost in Supabase, familiar UX, satisfies App Store with Google, enough for sync.
2. **Magic link only (email)** — no Google/Apple. Rejected: worse mobile UX; email is still PII; no one-tap sign-in.
3. **Anonymous account + later linking** — less PII at start. Rejected for MVP: weaker multi-device from day one; complex linking; when Google is added on iOS, Apple is required anyway.
4. **Apple only (all platforms)** — fewer email-PII options (Hide My Email). Rejected as sole IdP: worse UX on Android/web; Google is natural in the Android ecosystem.
5. **Email + password** — full control. Rejected at start: password reset, credential storage, higher maintenance vs OAuth.

## Consequences

- Positive: clear happy path on three clients; App Store alignment; consistent with minimum PII and ADR 0002; little custom auth logic.
- Negative: email in Auth (compromise vs “zero PII”); Google Cloud + Apple Developer + redirect URI config at scaffolding; two sign-in buttons on iOS.
- Later: IdP console checklist at scaffold; account-deletion implementation (SQL cascade / Edge Function); optional IdP linking when a user has separate Google and Apple accounts.

## Related

- [auth](../architecture/auth.md)
- [OQ-02](../open-questions.md) (decided)
- [ADR 0002](0002-backend-supabase.md)
- [data-model — profiles](../domain/data-model.md)
- [First migration spec — profiles](../specs/first-migration-profiles-rls.md)
- Follow-ups: IdP [#12](https://github.com/MarekBiolik/GymManager/issues/12), account deletion [#13](https://github.com/MarekBiolik/GymManager/issues/13), apply SQL [#21](https://github.com/MarekBiolik/GymManager/issues/21)
