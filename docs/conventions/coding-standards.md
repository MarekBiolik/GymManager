# Standardy kodu

Konwencje bazowe. Konkretne wersje linterów i formatters — przy scaffoldingu (faza 1).

## Zasady ogólne

1. **Logika domenowa tylko w `core`** — UI nie duplikuje reguł biznesowych (wyjątki tylko po ADR, zwłaszcza web / OQ-01).
2. **Jawne nazwy** — zgodne ze [słownikiem](../domain/glossary.md); unikamy skrótów typu `usr`, `sess2`.
3. **Małe PR-y** — jedna intencja; scaffolding oddzielnie od feature.
4. **Sekrety** — nigdy w repo; `service_role` nigdy w klientach.
5. **Dokumentacja** — zmiana architektury = ADR lub aktualizacja `docs/` w tym samym PR.

## Kotlin (core + Android)

- Idiomatyczny Kotlin; preferuj immutable data / value types w domenie.
- Publiczne API `core` możliwie wąskie (facade / use-case), reszta `internal`.
- Coroutines do I/O; unikaj blokowania wątku UI.
- Formatter/linter: ktlint lub spotless (do wyboru w fazie 1).

## Swift (iOS)

- SwiftUI + czytelne `View` bez logiki biznesowej.
- Model UI oddzielony od typów domenowych core (mapowanie na brzegu).
- SwiftFormat / SwiftLint — do włączenia w fazie 1.

## TypeScript / React (web)

- TypeScript strict.
- Funkcyjne komponenty; stan serwerowy / sync przez uzgodniony kontrakt (core lub supabase-js po OQ-01).
- ESLint + Prettier.
- Brak `any` bez komentarza uzasadniającego.

## SQL / Supabase

- Każda tabela użytkownika: `user_id` + polityki RLS w tej samej migracji.
- Nazwy tabel: `snake_case`, liczba mnoga (`food_entries`, `measurements`).
- Migracje tylko do przodu; zakaz ręcznego dryfu schematu prod.

## Komentarze

- Komentarz wyjaśnia **dlaczego**, nie co widać w kodzie.
- TODO z ID (`OQ-NN` lub issue), nie gołe `TODO: fix later`.
