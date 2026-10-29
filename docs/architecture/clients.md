# Klienci (iOS / Android / web)

Każdy klient to **cienka warstwa UI** nad wspólnym core. Wspólna decyzja UI: [ADR 0003](../decisions/0003-native-ui-per-platform.md).

## Android — `apps/android`

- UI: Jetpack Compose
- Język: Kotlin
- Integracja z core: zależność Gradle na moduł KMP (najprostsza ścieżka ze wszystkich platform)
- Odpowiedzialność lokalna: Activity/Navigation, motyw, uprawnienia systemowe, ewentualnie WorkManager do sync w tle

## iOS — `apps/ios`

- UI: SwiftUI
- Język: Swift
- Integracja z core: framework wygenerowany z KMP (XCFramework / SPM) — szczegóły przy scaffoldingu
- Odpowiedzialność lokalna: nawigacja SwiftUI, Keychain (jeśli tokeny poza SDK), signing, App Store
- Auth: przy publikacji z Google Sign-In App Store wymaga też **Sign in with Apple** — patrz [auth](auth.md)

## Web — `apps/web`

- UI: React + TypeScript + Vite
- Integracja z core: **otwarte** ([OQ-01](../open-questions.md))

### Ryzyko interop web ↔ KMP

KMP dobrze spina mobile. Na webie opcje:

| Ścieżka | Zalety | Wady |
|---------|--------|------|
| A. Core → JS/Wasm używany z React | Jedna logika domenowa | Dojrzałość toolchainu, DX, rozmiar bundla |
| B. Web → `supabase-js` bezpośrednio + logika w TS | Prosty DX, dojrzały ekosystem | Ryzyko **duplikacji** reguł względem core |
| C. Hybryda | Elastyczność | Trzeba jasno narysować, co wolno duplikować |

Do zamknięcia OQ-01 obowiązuje zasada: **nie rozbudowujemy logiki domenowej tylko w webie „na skróty”** bez ADR.

## Wspólne wymagania wobec klientów

1. Brak tajemnic w repo (klucze Supabase anon + URL OK w kliencie; service role nigdy).
2. Stan użytkownika (zalogowany / dane) pochodzi z core lub uzgodnionego kontraktu auth.
3. Feature flags / środowisko (dev/staging/prod) — konwencja w fazie 1.
4. Minimalny „hello”: login + odczyt/zapis jednej encji chronionej RLS — cel fazy 2.

## Poza zakresem UI (na teraz)

- Finalny design system i branding
- Szczegóły przełączania modułów żywienie/trening/pomiary ([OQ-08](../open-questions.md))
- PWA / instalacja na home screen jako zamiennik natywnego iOS
