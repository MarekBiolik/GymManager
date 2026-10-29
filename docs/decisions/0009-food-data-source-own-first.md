# ADR 0009 — Źródło danych żywnościowych: własne foods first (MVP)

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja zamykania OQ-07)

## Kontekst

Moduł żywienia (faza 3) potrzebuje katalogu produktów z makro, z których powstają **Food entry**. Stary prototyp polegał na OpenFoodFacts (OFF). Siły: solo maintenance, jakość danych (OFF bywa niepełne / błędne lokalnie), offline odczyt już zcache’owanych produktów ([ADR 0008](0008-sync-strategy-online-first.md)), brak zależności od zewnętrznego API w ścieżce krytycznej dziennika, szybki MVP bez integracji sieciowej do cudzej bazy.

Opcje z [OQ-07](../open-questions.md): tylko własne produkty, tylko OFF (live), hybryda (wyszukiwanie zewnętrzne + lokalne własne).

## Decyzja

1. **MVP (faza 3):** **własne foods only** — użytkownik tworzy i edytuje produkty w swojej tabeli `foods` (Postgres + RLS). Brak wywołań OpenFoodFacts (ani innego katalogu zewnętrznego) w kodzie MVP. Brak globalnej tabeli produktów współdzielonej między użytkownikami.
2. **SoT makro dla wpisów:** jak w [data-model](../domain/data-model.md) — `food_entries` trzyma **snapshot** makro w momencie wpisu; edycja `foods` nie przepisuje historii. Dziennik nie zależy od żywotności zewnętrznego API.
3. **Kanon wartości odżywczych w `foods` (MVP):** makro **na 100 g** + opcjonalne `default_serving_g` (domyślna porcja UI). Kalorie pochodne / spójność P+C+F — reguła w core (mobile) / cienka walidacja web; CHECK tylko na sensowne zakresy jeśli potrzeba ([postgres-invariants](../architecture/postgres-invariants.md)).
4. **Później (hybryda, poza MVP żywienia):** opcjonalne **wyszukiwanie / import** z OpenFoodFacts (lub innego źródła), które **kopiuje** nazwę i makro do **nowego wiersza `foods` użytkownika**. Po imporcie produkt jest zwykłym własnym Food — edytowalny, syncowany jak reszta. Kolumny provenance: `external_source` (np. `openfoodfacts`) + `external_id` (nullable); **nie** wymagane w runtime do odczytu dziennika.
5. **Barcode / live lookup przy każdym posiłku:** poza MVP. Jeśli kiedyś — nadal zapisuje lokalną kopię w `foods`, nie „link tylko do OFF”.

## Alternatywy rozważone

1. **Własne foods only, bez ścieżki hybrydy** — najprostsze. Odrzucone jako docelowy stan: UX wyszukiwania gotowych produktów jest wartościowy; zostawiamy **jasny** follow-up hybrydy zamiast zamykać drzwi.
2. **OFF-only (live API przy każdym wyborze)** — bogaty katalog od dnia 1. Odrzucone: jakość/kompletność PL, rate limit / ToS OFF, brak offline dla nowych produktów, utrzymanie klienta HTTP + mapowań, historia zależna od zewnętrznego ID.
3. **Hybryda od dnia 1 (search OFF + własne)** — wygodne UX wcześniej. Odrzucone na MVP: podwaja zakres fazy 3 (UI search, mapowanie pól, błędy sieci, licencje/atrybucja) zanim działa CRUD własnych foods + diary + agregacje.
4. **Wspólny katalog systemowy w Postgresie (curated)** — kontrola jakości. Odrzucone na start: koszt utrzymania treści solo; nie rozwiązuje „szybkiego dodania jogurtu z sklepu” bez i tak ręcznego wpisu lub importu.

## Konsekwencje

### Pozytywne

- Faza 3 buduje się na już ustalonym modelu (`foods` / `food_entries`, RLS, LWW) bez nowej integracji.
- Offline: raz zsyncowane `foods` są w read cache ([ADR 0008](0008-sync-strategy-online-first.md)); brak zależności od OFF przy logowaniu posiłku (zapis nadal online-only w MVP sync).
- Jakość danych pod kontrolą użytkownika; błędny import OFF da się poprawić lokalnie.
- Hybryda później nie wymaga migracji mentalnej: `external_*` to opcjonalny provenance na własnym wierszu.

### Negatywne / koszty

- MVP wymaga ręcznego dodawania produktów (tarcia UX vs trackery z pełnym OFF).
- Duplikaty nazw u jednego usera możliwe — dedupe / merge później, nie w MVP.
- Import OFF (gdy przyjdzie) musi respektować ToS / atrybucję i nie cache’ować całej bazy OFF w naszym Postgresie jako globalnego SoT.

### MVP vs później (granica)

| W zakresie MVP (faza 3) | Poza MVP (follow-up) |
|-------------------------|----------------------|
| CRUD `foods` (user-owned) | Wyszukiwanie OFF / barcode → import do `foods` |
| `food_entries` + snapshot makro | Live re-fetch OFF przy otwarciu produktu |
| Makro na 100 g + `default_serving_g` | Współdzielony katalog systemowy / curated DB |
| Nullable `external_source` / `external_id` w schemacie (mogą zostać nieużywane) | Inne źródła (USDA, lokalne DB sklepów) |
| Agregacje dzienne z wpisów | Community sharing produktów między userami |

Kolumny `external_*` **wolno** dodać już przy migracji żywienia (null), żeby uniknąć drugiej migracji — albo dodać dopiero przy implemencie hybrydy; preferencja przy specu fazy 3: **dodać nullable od razu**, bez UI/API OFF.

## Powiązane

- Zamyka [OQ-07](../open-questions.md)
- [data-model](../domain/data-model.md), [glossary](../domain/glossary.md), [roadmap](../roadmap.md) faza 3
- [legacy-cleanup](../conventions/legacy-cleanup.md) — OFF nie wraca przez stary serwer
- [ADR 0008](0008-sync-strategy-online-first.md) — sync / cache odczytu `foods`
- [postgres-invariants](../architecture/postgres-invariants.md) — ownership `food_entries` → `foods`
- Follow-up: [#26 Spec: import/wyszukiwanie OpenFoodFacts (hybryda post-MVP)](https://github.com/MarekBiolik/GymManager/issues/26)
