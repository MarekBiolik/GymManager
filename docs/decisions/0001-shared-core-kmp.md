# ADR 0001 — Wspólny core: Kotlin Multiplatform

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja planowania architektury)

## Kontekst

Aplikacja ma działać na Androidzie, iOS i webie. Chcemy profesjonalnego utrzymania: logika domenowa (żywienie, trening, pomiary, sync) nie może być kopiowana w trzech UI. Preferowany model: **wspólny core + natywne UI per platforma** (nie jeden cross-platform UI).

## Decyzja

Budujemy wspólny moduł **`core` w Kotlin Multiplatform (KMP)** z regułami domenowymi, use-case'ami i dostępem do danych (Supabase). Android i iOS konsumują core natywnie; web na dzień 1 **nie** linkuje binary KMP — `supabase-js` + cienka TS ([ADR 0006](0006-web-supabase-js-without-kmp-js.md), zamyka OQ-01).

## Alternatywy rozważone

1. **Expo / React Native (+ web)** — jeden UI na wszystkie platformy, mocny DX TypeScript. Odrzucone: mniej natywne UI; niespójne z wybranym modelem „core + native UI”.
2. **Flutter** — jeden UI, dojrzały multiplatform. Odrzucone: Dart zamiast Kotlin/Swift/TS; porzuca natywne UI per platforma.
3. **Tylko backend jako „core” (chudzi klienci bez shared lib)** — proste, ale reguły offline i współdzielona walidacja wracają do duplikacji albo uzależniają UX od sieci.
4. **Kotlin Multiplatform** — wybrany: jeden język core bliski Androidowi, oficjalne wsparcie iOS, ścieżka JS/Wasm dla web (z ryzykiem).

## Konsekwencje

- Pozytywne: jedna implementacja reguł; testy jednostkowe w core; Android integruje się najtaniej.
- Negatywne: trzeba utrzymać toolchain KMP + bindingi Swift; web bez shared binary wymaga dyscypliny domenowej (ADR 0006).
- Później: scaffolding `core/`, publiczne API use-case'ów; revisit KMP-JS tylko wg kryteriów ADR 0006.

## Powiązane

- [shared-core](../architecture/shared-core.md)
- [ADR 0003](0003-native-ui-per-platform.md)
- [ADR 0006](0006-web-supabase-js-without-kmp-js.md) (OQ-01)
