# ADR 0007 — Providery logowania i minimum PII

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja zamykania OQ-02)

## Kontekst

Potrzebujemy konta do **sync między urządzeniami** i izolacji danych (RLS), bez budowania profilu społecznościowego. Siły: wygoda logowania, koszt utrzymania (Supabase Auth, [ADR 0002](0002-backend-supabase.md)), **minimum PII**, wymagania **App Store** (Sign in with Apple, gdy oferujemy inny IdP trzeciej strony — m.in. Google).

Opcje z [OQ-02](../open-questions.md): Google OAuth, Sign in with Apple, magic link, konto anonimowe + późniejsze linkowanie.

## Decyzja

1. **Providery:** **Google** (Android, iOS, web) oraz **Sign in with Apple** na **iOS** (wymagane przy Google w App Store). Na Androidzie i webie wystarczy Google; Apple na web/Android **nie** w MVP.
2. **Sesja:** Supabase Auth (JWT + odświeżanie przez SDK). Jedno konto = te same dane na wszystkich klientach.
3. **Minimum PII:** nie zbieramy imienia, płci, daty urodzenia, telefonu ani adresu. Email trafia do `auth.users` z IdP (nieuniknione przy Google/Apple) — **nie** kopiujemy go do `profiles`, **nie** używamy do marketingu. Brak analytics identyfikujących osobę na start.
4. **`profiles`:** rekord techniczny 1:1 z `auth.users` — `id`, znaczniki czasu, preferencje aplikacji (np. jednostki). Bez display name / avatara z IdP.
5. **Usuwanie konta:** użytkownik inicjuje w ustawieniach; usuwamy dane aplikacji (`user_id`) oraz rekord Auth — zarys w [auth](../architecture/auth.md#usuwanie-konta).

**Poza MVP:** magic link, rejestracja email+hasło, konta anonimowe, linkowanie wielu IdP poza domyślnym zachowaniem Supabase (np. ten sam email Google↔Apple).

## Alternatywy rozważone

1. **Google + Apple (iOS)** — wybrane: niski koszt w Supabase, znany UX, spełnia App Store przy Google, wystarcza do sync.
2. **Tylko magic link (email)** — bez Google/Apple. Odrzucone: gorszy UX mobilny; email i tak jest PII; brak „jednym kliknięciem”.
3. **Anonimowe konto + późniejsze linkowanie** — mniej PII na start. Odrzucone na MVP: słabszy multi-device od pierwszego dnia; złożone linkowanie; gdy dodamy Google na iOS, i tak wymagane Apple.
4. **Tylko Apple (wszystkie platformy)** — mniej PII-opcji email (Hide My Email). Odrzucone jako jedyny IdP: gorszy UX na Androidzie/webie; Google i tak naturalny w ekosystemie Android.
5. **Email + hasło** — pełna kontrola. Odrzucone na start: reset hasła, przechowywanie credentiali, większy koszt utrzymania vs OAuth.

## Konsekwencje

- Pozytywne: jasny happy path na trzech klientach; zgodność z App Store; spójne z minimum PII i ADR 0002; mało własnej logiki auth.
- Negatywne: email w Auth (kompromis vs „zero PII”); konfiguracja Google Cloud + Apple Developer + redirect URI przy scaffoldingu; na iOS dwa przyciski logowania.
- Później: checklista konsol IdP przy scaffoldzie; implementacja usuwania konta (SQL cascade / Edge Function); ewentualne linkowanie IdP gdy użytkownik ma osobne konta Google i Apple.

## Powiązane

- [auth](../architecture/auth.md)
- [OQ-02](../open-questions.md) (decided)
- [ADR 0002](0002-backend-supabase.md)
- [data-model — profiles](../domain/data-model.md)
- Issue follow-up: konfiguracja IdP przy scaffoldingu / usuwanie konta w kodzie
