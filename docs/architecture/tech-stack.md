# Stack technologiczny

Wersje konkretnych bibliotek zostaną przypięte przy scaffoldingu (faza 1). Poniżej — wybór technologii i rola.

## Wspólny core

| Technologia | Rola |
|-------------|------|
| Kotlin Multiplatform (KMP) | Wspólny kod domeny, danych, sync |
| Kotlin Coroutines / Flow | Asynchroniczność i strumienie stanu |
| kotlinx.serialization | Serializacja modeli |
| supabase-kt (community) | Klient Supabase z core (Android/iOS; web — zależnie od OQ-01) |

Szczegóły granic: [shared-core](shared-core.md). Decyzja: [ADR 0001](../decisions/0001-shared-core-kmp.md).

## Android

| Technologia | Rola |
|-------------|------|
| Kotlin | Język aplikacji |
| Jetpack Compose | UI |
| Android Gradle Plugin | Build |
| core (KMP) jako zależność | Logika |

## iOS

| Technologia | Rola |
|-------------|------|
| Swift | Język aplikacji |
| SwiftUI | UI |
| Xcode | Build / signing |
| core jako XCFramework / SPM z KMP | Logika |

Wymaga konta **Apple Developer Program** (publikacja App Store).

## Web

| Technologia | Rola |
|-------------|------|
| TypeScript | Język |
| React | UI |
| Vite | Tooling / build |
| supabase-js | Klient Supabase (ścieżka bazowa; finalny model z core: OQ-01) |

## Backend / dane

| Technologia | Rola |
|-------------|------|
| Supabase | BaaS: Auth, Postgres, RLS, Realtime, Storage |
| PostgreSQL | Relacyjna baza (historia, agregacje, FK) |
| Supabase Auth | Logowanie (kierunek: Google + Apple na iOS) |
| SQL migrations w `supabase/` | Schemat jako kod |

Decyzja: [ADR 0002](../decisions/0002-backend-supabase.md). Hosting: [OQ-05](../open-questions.md).

## Narzędzia projektowe (zarys)

| Obszar | Kierunek | Status |
|--------|----------|--------|
| Monorepo | Root z `core/` + `apps/*` + `supabase/` + `docs/` | OQ-04 |
| CI | Build core + klienci + lint; migracje na PR | [ci-cd](../conventions/ci-cd.md) |
| Testy | Unit w core; UI testy per klient później | [testing](../conventions/testing.md) |
| Formatowanie | ktlint / spotless (Kotlin); ESLint+Prettier (web); SwiftFormat (iOS) | do przypięcia w fazie 1 |

## Świadomie nie wybrano (na teraz)

- Flutter / React Native jako główny UI — odrzucone na rzecz natywnego UI ([ADR 0003](../decisions/0003-native-ui-per-platform.md)).
- Firebase / Firestore — gorszy fit do agregacji i silniejszy lock-in.
- Własny backend Node od zera — za dużo utrzymania na start.
- Warstwa AI — faza 7 roadmapy.
