# Spec: kolejka offline-write i revisit konfliktów (post-MVP)

**Status:** SPEC ONLY — **bez implementacji**. Świadomie **poza MVP**, **nie faza 1**, **nie faza 2** („hello sync”).  
**Issue:** [#20](https://github.com/MarekBiolik/GymManager/issues/20)  
**Baseline sync (MVP):** [ADR 0008](../decisions/0008-sync-strategy-online-first.md) — online-first + cache odczytu; zapis tylko online.  
**Implementacja:** dopiero po triggerze produktowym poniżej; wtedy **osobny ADR** (lub revisit ADR 0008) + kod. Ten dokument to mapa decyzji, nie zezwolenie na kod w fazach 1–2.

## Cele

1. Zamknąć lukę „co robimy, gdy zapis bez sieci stanie się wymaganiem” — bez budowania kolejki teraz.
2. Ustalić **trigger produktowy** startu tematu.
3. Zarysować **model kolejki** (lokalny store, ACK, retry, idempotencja).
4. Ustalić kierunek **tombstone / soft-delete** vs hard delete.
5. Udokumentować **obowiązkowy revisit reguł konfliktów** przy lądowaniu offline-write (LWW vs per typ encji).
6. Odnotować wpływ na **web** i warunek revisit [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md).

## Non-goals

| Poza zakresem | Gdzie / kiedy |
|---------------|----------------|
| Implementacja kolejki / optimistic UI / tombstone w kodzie | po triggerze + nowym ADR |
| Zmiana baseline MVP (zapis tylko online) | [ADR 0008](../decisions/0008-sync-strategy-online-first.md) — zostaje |
| Kontrakt cache odczytu / hello sync (faza 2) | [#19](https://github.com/MarekBiolik/GymManager/issues/19) |
| Scaffold monorepo / CI / pierwsza migracja | faza 1: [#14](https://github.com/MarekBiolik/GymManager/issues/14), [#6](https://github.com/MarekBiolik/GymManager/issues/6), [#21](https://github.com/MarekBiolik/GymManager/issues/21) |
| Obowiązkowy Realtime jako bus sync | nadal odrzucone jako wymóg ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |
| Wspólny silnik KMP→JS/Wasm „żeby mieć kolejkę na webie” | tylko przy spełnieniu warunków revisit ADR 0006 |

## Trigger produktowy (kiedy startujemy)

Temat **nie** startuje „przy okazji” fazy 1–2 ani przy pierwszym module domenowym. Startujemy, gdy **co najmniej jeden** warunek jest prawdziwy:

1. **Ból zapisu offline** — użytkownik (nawet solo) regularnie traci trening / wpis żywieniowy / pomiar przez brak sieci w siłowni lub metrze, i to blokuje codzienny użytek MVP.
2. **Cel produktowy „airplane mode”** — świadoma decyzja roadmapy (np. po fazie 4 trening), że zapis offline jest wymaganiem wydania, nie nice-to-have.
3. **Dane lokalne jako commit** — UI wymaga optimistic / lokalnego commit przed ACK (nie wystarczy banner „brak sieci”).

Do czasu triggera: zachowanie MVP z ADR 0008 (odrzut zapisu offline z komunikatem) pozostaje obowiązujące.

**Kolejność względem faz:** najwcześniej sensownie **po** stabilnym hello sync (#19) i po realnym module treningu (faza 4) albo wcześniej tylko przy twardym bólu (1). **Nigdy** w fazie 1 scaffold.

## Model kolejki (zarys — do dopięcia w ADR implementacyjnym)

Postgres pozostaje **jedyną SoT** ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)). Lokalna kolejka to **bufor mutacji**, nie druga baza prawdy.

### Co store’ujemy lokalnie (per pozycja kolejki)

| Pole (kierunek) | Rola |
|-----------------|------|
| `client_mutation_id` (UUID) | Idempotencja — ten sam ID przy retry; serwer / klient deduplikuje |
| `entity_table` + `entity_id` | Cel mutacji (np. `sessions`, `sets`, `food_entries`) |
| `op` | `insert` \| `update` \| `delete` |
| `payload` | Snapshot wiersza / delty wystarczającej do replay |
| `base_updated_at` | `updated_at` widziane przy edycji (wykrycie konfliktu przy sync) |
| `enqueued_at` / `attempts` / `last_error` | Retry i diagnostyka |
| `status` | `pending` \| `in_flight` \| `acked` \| `failed_terminal` |

Cache odczytu (#19) i kolejka zapisów to **osobne** concerny: kolejka nie zastępuje SoT; po ACK aktualizujemy cache jak przy zapisie online.

### ACK

1. Klient wysyła mutację z `client_mutation_id` gdy sieć wraca (FIFO per użytkownik / per encja — decyzja w ADR implementacyjnym; domyślnie **kolejność FIFO globalna** na urządzeniu, z możliwością późniejszego zawężenia).
2. Sukces API (2xx + RLS) → status `acked`, usunięcie lub archiwizacja pozycji, odświeżenie cache.
3. Konflikt LWW / odrzut wersji → ścieżka conflict revisit (poniżej), nie ciche „zgub mutację” bez polityki.

### Retry

- Retry z backoffem przy błędach sieci / 5xx; **ten sam** `client_mutation_id`.
- Błędy 4xx merytoryczne (RLS, walidacja) → `failed_terminal` + UI (nie nieskończony retry).
- Limit prób i „wymaga interwencji użytkownika” — w ADR implementacyjnym.

### Idempotencja

- Klient: stabilny `client_mutation_id` per mutacja.
- Serwer (kierunek): unikalność `(user_id, client_mutation_id)` albo równoważny log mutacji — **nie** w pierwszej migracji MVP (#6); dopiero migracja towarzysząca offline-write.

```text
enqueue(mutation)
  persist local queue row (pending)
  if online: flush()
  else: UI shows pending / offline

flush()
  for each pending in FIFO:
    POST/PATCH/DELETE with client_mutation_id
    on 2xx → acked + update read cache
    on conflict → apply conflict policy (revisit)
    on retryable → backoff, keep pending
```

## Tombstone / soft-delete vs hard delete

| Faza | Usuwanie |
|------|----------|
| MVP (obecne) | **Hard delete** w Postgresie + invalidacja cache ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |
| Przy offline-write | **Tombstone / soft-delete** (lub równoważny tombstone sync) — wymagane, żeby delete z kolejki i delete z drugiego urządzenia dały się uzgodnić bez „wskrzeszania” wiersza |

Kierunek przy lądowaniu offline-write:

1. Kolumna typu `deleted_at timestamptz NULL` (lub `is_deleted` + `deleted_at`) na tabelach użytkownika objętych kolejką.
2. Sync traktuje tombstone jako mutację jak update (LWW na wiersz / reguła z revisit).
3. Twarde czyszczenie (GC) tombstone’ów — później, z retencją; nie blokuje pierwszego ADR offline-write.
4. Pierwsza migracja MVP (#6) **nie** dodaje tombstone’ów z wyprzedzeniem — unikamy schematu „na zapas”.

## Konflikty — obowiązkowy revisit przy offline-write

MVP świadomie trzyma **jedną** regułę: LWW na wiersz wg `updated_at` dla wszystkich typów encji ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)).

**Przy starcie implementacji offline-write revisit konfliktów jest obowiązkowy** (osobna sekcja w ADR implementacyjnym lub osobny ADR) — nie wolno „dorzucić kolejki” przy założeniu, że LWW wystarczy bez sprawdzenia.

### Pytania do zamknięcia w tamtym ADR

1. Czy **zostajemy przy jednej regule LWW** na cały wiersz dla żywienia / treningu / pomiarów / `profiles`?
2. Czy wprowadzamy **reguły per typ encji** (np. merge serii w sesji, LWW na pomiarze wagi, append-only na `food_entries`)?
3. Czy potrzebne jest **UI „resolve conflict”**, czy nadal ciche reguły + ewentualny banner „nadpisano lokalną zmianę”?
4. Jak `base_updated_at` w kolejce mapuje się na odrzut / rebase / drop mutacji?

### Rekomendowany punkt startu revisit (nie decyzja końcowa)

| Typ encji (kierunek) | Hipoteza na start revisit | Dlaczego |
|----------------------|---------------------------|----------|
| `profiles`, pomiary skalarne | LWW na wiersz | Proste; rzadkie równoległe edycje |
| Wpisy żywieniowe / serie | LWW lub append-only (osobne ID wiersza) | Unikać merge per-pole bez dowodu bólu |
| Agregaty sesji treningowej | Kandydat na merge / „union sets” jeśli LWW kasuje serie z drugiego urządzenia | Najbardziej prawdopodobny ból przy offline-write |

Domyślna hipoteza: **zostać przy LWW**, dopóki konkretny scenariusz (zwłaszcza trening offline na dwóch urządzeniach) nie udowodni potrzeby reguły per encja. Revisit **dokumentuje wybór** — nawet jeśli wybór to „nadal LWW wszędzie”.

## Wpływ na web (ADR 0006)

Web dziś: `supabase-js` + cienki cache; **bez** silnika sync z KMP ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)).

Przy offline-write:

| Opcja | Skutek |
|-------|--------|
| A. Offline-write **tylko mobile** (KMP `core/sync`) | Web nadal online-first; dokumentować lukę UX; prostsze |
| B. Ta sama semantyka kolejki na webie (osobna implementacja TS) | Koszt duplikacji; warunek **parity** zbliża do revisit ADR 0006 pkt 2 |
| C. Współdzielony silnik (KMP→JS/Wasm) | Tylko gdy warunki revisit ADR 0006 spełnione (parity + toolchain + ból duplikacji) |

**Odnotowanie (zamknięte w tym specu):** wprowadzenie offline-write **musi** jawnie wybrać A/B/C w ADR implementacyjnym. Domyślna preferencja do czasu revisit toolchainu: **A (mobile-first)** albo **B tylko dla minimalnego podzbioru mutacji**, jeśli web ma ten sam ból produktowy. Wybór B lub „pełna parity” **odpala ocenę** warunku 2 w ADR 0006 — nie implikuje automatycznie Wasm.

## Relacja do ADR 0008

ADR 0008 pozostaje **accepted** dla MVP. Ten spec:

- **nie** zmienia SoT, LWW MVP ani „zapis tylko online”;
- **wypełnia** sekcję „Poza MVP” konkretnym planem kolejki / tombstone / revisit konfliktów / web;
- wymaga **nowego ADR** (lub formalnego revisit 0008) **przed** kodem kolejki.

## Powiązane

- [ADR 0008](../decisions/0008-sync-strategy-online-first.md), [data-and-sync](../architecture/data-and-sync.md)
- [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md) — parity / revisit
- [#19](https://github.com/MarekBiolik/GymManager/issues/19) — read-cache / hello sync (faza 2; osobny tor)
- [#6](https://github.com/MarekBiolik/GymManager/issues/6) / [#21](https://github.com/MarekBiolik/GymManager/issues/21) — migracja MVP bez tombstone / bez tabeli mutacji
- [shared-core](../architecture/shared-core.md) — `core/sync/` = cache teraz; kolejka dopiero po tym specu + ADR
