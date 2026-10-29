# ADR 0008 — Strategia sync: online-first + read cache (MVP)

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja zamykania OQ-03)

## Kontekst

Tracker fitness potrzebuje **tych samych danych** na Androidzie, iOS i webie po logowaniu ([ADR 0007](0007-auth-providers-minimum-pii.md)). Backend to Postgres przez Supabase ([ADR 0002](0002-backend-supabase.md)). Siły: solo maintenance, szybki baseline fazy 2 („hello sync”), unikanie złożoności kolejek offline i zegarów na starcie, przy zachowaniu ścieżki do offline-write gdy siłownia bez zasięgu stanie się realnym bólem.

Opcje z [OQ-03](../open-questions.md) / wcześniejszego zarysu w data-and-sync: online-only, online-first + cache odczytu, local-first + kolejka zapisów, Realtime między urządzeniami.

Web nie współdzieli silnika sync z KMP ([ADR 0006](0006-web-supabase-js-without-kmp-js.md)) — wspólna prawda musi żyć w **Postgres + RLS**, nie w drugiej lokalnej bazie jako SoT.

## Decyzja

1. **Source of truth:** **PostgreSQL (Supabase)**. Każdy udany zapis klienta ląduje w Postgresie (przez RLS + JWT `user_id` = konto Auth z ADR 0007). Lokalny store **nie** jest drugą prawdą.
2. **MVP baseline:** **online-first + lokalny cache odczytu**. Odczyt: sieć gdy dostępna, inaczej świeży cache (stale-while-revalidate / „pokaż ostatnio pobrane”). Zapis: **tylko gdy online** — request do API; przy braku sieci UI blokuje zapis z jasnym komunikatem (bez cichej kolejki).
3. **Offline-write (kolejka, optimistic UI z trwałym lokalnym commit):** **poza MVP** — follow-up gdy moduły (zwłaszcza trening w siłowni) tego wymagają; wtedy osobny ADR/revisit konfliktów.
4. **Konflikty (MVP):** jedna reguła dla wszystkich typów encji użytkownika (żywienie / trening / pomiary / `profiles`): **last-write-wins (LWW) na wiersz** wg `updated_at` (timestamptz z serwera / uzgodnionego zegara zapisu). Brak merge per-pole na start. Przy online-first konflikty są rzadkie (głównie dwa urządzenia online naraz); LWW wystarcza.
5. **Realtime Supabase:** **opcjonalne** w fazie 2+ do odświeżania UI gdy drugie urządzenie zapisze — nie wymagane do akceptacji „hello sync”. Subskrypcje tylko dla tabel użytkownika, zawsze przez RLS.
6. **Klienci:** mobile — repozytoria w KMP czytają/piszą Supabase (`supabase-kt`) + cache odczytu w core; web — to samo zachowanie przez `supabase-js` + cienki cache przeglądarki/pamięci, **bez** równoległej domeny sync w TS ([ADR 0006](0006-web-supabase-js-without-kmp-js.md)).
7. **Usuwanie:** MVP = **hard delete** wiersza w Postgresie + invalidacja cache. Tombstone / soft-delete pod sync offline — dopiero przy offline-write.

## Alternatywy rozważone

1. **Online-first + read cache (wybrane)** — prosty model mentalny; SoT = Postgres; spójne z web bez KMP; wystarcza do multi-device po zalogowaniu. Wady: brak zapisu w siłowni bez sieci.
2. **Czyste online-only (bez cache)** — jeszcze prostsze. Odrzucone jako baseline UX: zbędne spinnery przy każdym wejściu w historię; cache odczytu to mały koszt.
3. **Local-first + kolejka zapisów** — najlepszy UX offline. Odrzucone na MVP: kolejka, idempotencja, konflikty, zegary, tombstone’y, parity web↔mobile — za drogie przed działającym auth/RLS/hello sync.
4. **Merge per typ encji od dnia 1** (np. scalanie serii w sesji) — precyzyjniejsze. Odrzucone na start: brak dowodu, że LWW boli; jedna reguła mniej kodu i mniej dryfu web/mobile.
5. **Realtime jako obowiązkowy silnik sync** — live wszędzie. Odrzucone jako wymóg MVP: pull/refresh + zapis REST wystarcza; Realtime = opt-in ulepszenie.

## Konsekwencje

### Pozytywne

- Faza 2 może zbudować „hello sync” bez silnika offline.
- Jedna SoT i jedna reguła konfliktów — łatwe do opisania w pierwszej migracji (`updated_at`, RLS).
- Spójne z tożsamością ADR 0007 i ścieżką web ADR 0006 (oba klienty piszą do tego samego Postgresa).
- Jasny trigger revisit: realny ból zapisu offline.

### Negatywne / koszty

- W siłowni bez zasięgu użytkownik **nie** zapisze treningu / posiłku w MVP.
- Dwa urządzenia edytujące ten sam wiersz online → ciche LWW (ostatni `updated_at` wygrywa); brak UI „resolve conflict”.
- Cache odczytu może pokazać dane nieaktualne do następnego udanego fetch — UI powinno to tolerować (np. wskaźnik „offline / ostatnia aktualizacja”).
- Pakiet `core/sync/` na start to **cienki** helper cache/invalidacji, nie pełna kolejka — nazwa katalogu nie obiecuje offline-write.

### Wymagania schematu (wejście do #6)

- Każda tabela użytkownika: `user_id`, `created_at`, `updated_at` (timestamptz).
- Zapisy aktualizują `updated_at` (trigger lub jawny SET w kliencie — decyzja przy migracji).
- Brak obowiązkowych kolumn tombstone w MVP.

### Zachowanie offline (MVP) — skrót

| Operacja | Online | Offline |
|----------|--------|---------|
| Odczyt listy / szczegółu | Fetch → odśwież cache | Pokaż cache; jeśli pusty — pusty stan + „brak sieci” |
| Zapis / update / delete | API + RLS; przy sukcesie update cache | **Odrzuć** z komunikatem; nie kolejkuj |
| Logowanie / odświeżenie sesji | Jak auth.md | Bez ważnej sesji brak dostępu do danych chronionych |

### Poza MVP (świadomie później)

- Kolejka zapisów offline + idempotentne requesty
- Optimistic UI z lokalnym commit przed ACK serwera
- Merge / CRDT / per-encja conflict UI
- Obowiązkowy Realtime jako bus sync
- Wspólny silnik offline web+mobile (warunek revisit ADR 0006)

## Powiązane

- Zamyka [OQ-03](../open-questions.md)
- [data-and-sync](../architecture/data-and-sync.md)
- [ADR 0002](0002-backend-supabase.md), [ADR 0006](0006-web-supabase-js-without-kmp-js.md), [ADR 0007](0007-auth-providers-minimum-pii.md)
- [data-model](../domain/data-model.md), [auth](../architecture/auth.md)
- Issue #6 — pierwsza migracja (profiles + RLS + `updated_at`)
- Follow-upy: [#19 kontrakt read-cache / hello sync](https://github.com/MarekBiolik/GymManager/issues/19), [#20 offline-write post-MVP](https://github.com/MarekBiolik/GymManager/issues/20)
