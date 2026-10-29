# Klienci (iOS / Android / web)

Każdy klient to **cienka warstwa UI** nad wspólnym core (na webie: cienkie UI nad Supabase, bez binary KMP — [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)). Wspólna decyzja UI: [ADR 0003](../decisions/0003-native-ui-per-platform.md).

## Android — `apps/android`

- UI: Jetpack Compose
- Język: Kotlin
- Integracja z core: zależność Gradle na `:core` w tym samym multi-project ([ADR 0005](../decisions/0005-monorepo-tooling.md))
- Odpowiedzialność lokalna: Activity/Navigation, motyw, uprawnienia systemowe, ewentualnie WorkManager do sync w tle

## iOS — `apps/ios`

- UI: SwiftUI
- Język: Swift
- Integracja z core: XCFramework / lokalny SPM wygenerowany z KMP (ten sam commit co app; kroki: [playbook fazy 1](../plans/phase-1-scaffold-playbook.md))
- Odpowiedzialność lokalna: nawigacja SwiftUI, Keychain (jeśli tokeny poza SDK), signing, App Store
- Auth: przy publikacji z Google Sign-In App Store wymaga też **Sign in with Apple** — patrz [auth](auth.md)

## Web — `apps/web`

- UI: React + TypeScript + Vite; workspace **pnpm** ([ADR 0005](../decisions/0005-monorepo-tooling.md))
- Integracja z core: **brak binary KMP na dzień 1** — web → Supabase przez `supabase-js` ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md); zamyka OQ-01); poza drzewem Gradle
- Odpowiedzialność lokalna: routing, motyw, session/auth glue (`supabase-js`), mapowanie wierszy/DTO na widoki, cienka walidacja UX

### Decyzja interop (OQ-01 → ADR 0006)

KMP spina **Android i iOS**. Web **nie** konsumuje tego samego binary co mobile, dopóki warunki revisit z ADR 0006 nie uzasadnią A/C.

| Ścieżka | Status | Zalety | Wady |
|---------|--------|--------|------|
| A. Core → JS/Wasm używany z React | odłożona (revisit) | Jedna logika domenowa | Toolchain, DX, bundle |
| B. Web → `supabase-js` + cienka TS | **wybrana (dzień 1)** | Prosty DX, dojrzały ekosystem | Ryzyko duplikacji reguł względem core |
| C. Hybryda | odłożona (revisit) | Elastyczność | Część kosztu A + dwa I/O |

### Co wolno / nie wolno w webie względem domeny

**Wolno:** `supabase-js` (RLS, anon key); UI i prezentacja; typy generowane ze schematu Supabase; lekka walidacja formularza lustrzana względem DB/core.

**Nie wolno:** zakładać KMP-JS/Wasm bez revisit/ADR; budować równoległą domenę TS (use-case’y, agregacje biznesowe); traktować walidacji tylko-w-TS jako security boundary; omijać RLS.

Wspólne niezmienniki potrzebne web + mobile → preferuj **Postgres (RLS / CHECK / constraints)**. Reguły bogate / offline-first → **KMP core** (mobile); na webie nie reimplementuj „na skróty”.

## Wspólne wymagania wobec klientów

1. Brak tajemnic w repo (klucze Supabase anon + URL OK w kliencie; service role nigdy).
2. Stan użytkownika (zalogowany / dane): mobile z core; web z sesji `supabase-js` / uzgodnionego kontraktu auth ([ADR 0007](../decisions/0007-auth-providers-minimum-pii.md)).
3. Feature flags / środowisko (`dev` day one; staging/prod później) — [playbook fazy 1](../plans/phase-1-scaffold-playbook.md), [ADR 0004](../decisions/0004-supabase-hosting.md).
4. Minimalny „hello”: login + odczyt/zapis jednej encji chronionej RLS (online-first + read cache — [ADR 0008](../decisions/0008-sync-strategy-online-first.md)) — cel fazy 2.

## Poza zakresem UI (na teraz)

- Finalny design system i branding
- Szczegóły przełączania modułów żywienie/trening/pomiary ([OQ-08](../open-questions.md))
- PWA / instalacja na home screen jako zamiennik natywnego iOS
