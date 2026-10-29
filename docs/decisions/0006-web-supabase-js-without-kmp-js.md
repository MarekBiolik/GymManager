# ADR 0006 — Web: supabase-js bez KMP→JS/Wasm (dzień 1)

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja zamykania OQ-01)

## Kontekst

Android i iOS konsumują wspólny core KMP natywnie ([ADR 0001](0001-shared-core-kmp.md)). Web (React + TypeScript) potrzebuje strategii wobec tego samego core. Trzy ścieżki z [OQ-01](../open-questions.md):

| Opcja | Zarys |
|-------|--------|
| **A** | Core skompilowany do JS/Wasm, konsumowany z React |
| **B** | Web → `supabase-js` + cienka logika TS |
| **C** | Hybryda: część domeny z shared (JS/Wasm), networking web osobno |

Siły: **solo maintenance**, szybki i przewidywalny scaffold (faza 1–2), unikanie kruchego toolchainu KMP-JS/Wasm na starcie, przy zachowaniu dyscypliny „jedna prawda domenowa” tam, gdzie to realnie boli.

## Decyzja

Wybieramy **B na dzień 1** (i przez scaffold + baseline auth/sync):

- `apps/web` łączy się z Supabase przez **`supabase-js`** (Auth, Postgres, Realtime, Storage).
- **Nie** dodajemy targetu JS/Wasm do `core` i **nie** zakładamy, że web konsumuje ten sam binary co mobile.
- Reguły domenowe nadal **żyją w KMP** dla Android/iOS; wspólne niezmienniki bezpieczeństwa i integralności danych **preferujemy w Postgres (RLS, CHECK, constraints)**, żeby web i mobile miały tę samą ochronę bez współdzielonego binary.
- W TS dozwolona jest tylko **cienka** warstwa: UI, routing, mapowanie DTO→widok, session glue, opcjonalna walidacja UX lustrzana względem constraintów serwera — **bez** pakietu „domain services” równoległego do core.

Ścieżka A (lub węższa C) pozostaje **opcją revisit**, nie celem scaffoldu.

## Alternatywy rozważone

1. **A — Core → JS/Wasm z React** — jedna implementacja reguł także na webie. Odrzucone na dzień 1: dojrzałość/DX toolchainu KMP-JS/Wasm + integracja z ekosystemem Vite/React i `supabase-kt` vs `supabase-js` to realny koszt dla jednego maintainera; opóźniałoby fazę 1–2 bez gwarancji stabilności.
2. **B — `supabase-js` + cienka TS** — wybrana: dojrzały DX, standardowy stack web, prosty scaffold, jasny rozdział. Koszt: ryzyko dryfu reguł, jeśli TS zacznie „dorastać” domenę — stąd twarde guardraile w docs klientów/core.
3. **C — Hybryda (domena shared + networking web)** — elastyczna, ale wymaga części A (target JS/Wasm) plus dwóch ścieżek I/O. Odrzucona na start jako złożoność bez korzyści, dopóki nie ma dowodu, że B boli; może być etapem pośrednim przy revisit w stronę A.

## Konsekwencje

### Pozytywne

- Scaffold web = zwykła aplikacja Vite/React + `supabase-js`; bez blokady na KMP-JS.
- Mobile nadal ma jeden core KMP; decyzja nie osłabia ADR 0001 dla Android/iOS.
- Wspólne reguły krytyczne można egzekwować w bazie (RLS/constraints) niezależnie od klienta.

### Negatywne / koszty

- Web **nie** współdzieli automatycznie use-case’ów ani sync engine z KMP — MVP sync to online-first po obu stronach ([ADR 0008](0008-sync-strategy-online-first.md)); parity **offline-write** web↔mobile może wymagać revisit A/C.
- Ryzyko duplikacji reguł w TS, jeśli dyscyplina guardrailów się rozmyje.
- Typy / DTO na webie pochodzą ze schematu Supabase (np. generowane typy), nie z modeli KMP — możliwy dryf nazw/kształtów; łagodzimy konwencją i migracjami jako źródłem prawdy schematu.

### Co wolno / czego nie wolno (web)

**Wolno:**

- Wołać Supabase przez `supabase-js` (zgodnie z RLS; tylko anon key + URL w kliencie).
- Trzymać stan UI, nawigację, formatowanie prezentacyjne.
- Generować typy TS ze schematu bazy.
- Dublować **lekką** walidację formularza wyłącznie jako UX (komunikaty przed submit), gdy ta sama reguła jest już w DB/core.

**Nie wolno:**

- Zakładać konsumpcji binary KMP (JS/Wasm) bez nowego ADR / revisit.
- Budować w `apps/web` warstwy domenowej równoległej do `core` (agregacje biznesowe, „kompletność sesji”, polityki żywieniowe itd.).
- Omijać RLS / pisać ścieżki „ufamy klientowi”.
- Traktować walidacji tylko-w-TS jako źródła prawdy bezpieczeństwa.

Szczegóły operacyjne: [clients](../architecture/clients.md), [shared-core](../architecture/shared-core.md).  
Podział reguł DB vs core/TS (checklista migracji): [postgres-invariants](../architecture/postgres-invariants.md).

### Warunki revisit (kiedy wrócić do A lub C)

Rozważamy ponownie KMP→JS/Wasm (A) lub hybrydę (C), gdy **co najmniej jeden** warunek jest spełniony:

1. **Ból duplikacji** — ta sama reguła biznesowa musi być utrzymywana w Kotlin i TS i powoduje realne bugi albo systematyczny dryf (nie jednorazowy helper UX).
2. **Parity offline-write** — web ma mieć ten sam model kolejki/konfliktów co mobile po wprowadzeniu offline-write (dziś MVP: online-first, [ADR 0008](0008-sync-strategy-online-first.md) / [OQ-03](../open-questions.md) decided), a utrzymanie dwóch silników jest droższe niż koszt toolchainu JS/Wasm.
3. **Toolchain dojrzały pod nas** — zielony, powtarzalny build `core` → JS lub Wasm wpinany w Vite, akceptowalny rozmiar bundla, i ścieżka danych zgodna z wybranym klientem Supabase — w praktyce sprawdzona spike’iem, nie założeniem.
4. **Web jako powierzchnia krytyczna** — produktowo web dogania mobile w złożoności domeny i koszt B przewyższa koszt A/C.

Do czasu revisit: **brak** ukrytego założenia w scaffoldzie, CI ani docs, że web „dostanie core za darmo”.

## Powiązane

- Zamyka [OQ-01](../open-questions.md)
- [ADR 0001](0001-shared-core-kmp.md), [ADR 0003](0003-native-ui-per-platform.md)
- [clients](../architecture/clients.md), [shared-core](../architecture/shared-core.md), [postgres-invariants](../architecture/postgres-invariants.md), [tech-stack](../architecture/tech-stack.md), [overview](../architecture/overview.md)
