# Otwarte pytania

Obszary świadomie **nie rozstrzygnięte** w iteracji 1. Każdy punkt powinien zostać zamknięty osobną iteracją (plan + ADR lub aktualizacja docs) przed głębokim kodowaniem.

Statusy: `open` | `in progress` | `decided` (z linkiem do ADR/docs).

---

## OQ-01 — Interop web ↔ wspólny core (KMP)

**Status:** decided — [ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md)  
**Dlaczego ważne:** KMP naturalnie spina Android i iOS; web (JS/Wasm) jest trudniejszy. Złe rozwiązanie = duplikacja logiki domenowej albo kruchy interop.

**Decyzja (dzień 1):** opcja **B** — web → `supabase-js` + cienka TS; **bez** KMP→JS/Wasm w scaffoldzie. Guardraile i warunki revisit: ADR 0006 + [clients](architecture/clients.md) / [shared-core](architecture/shared-core.md). Operacyjny podział reguł DB vs core/TS: [postgres-invariants](architecture/postgres-invariants.md) (issue #17).

**Opcje (rozważone):**

1. Core skompilowany do JS/Wasm, konsumowany przez React/TS.
2. Web gada wprost do Supabase przez `supabase-js`; cienka logika w TS (ryzyko duplikacji względem core) — **wybrane na start**.
3. Hybryda: domena/walidacje w shared (jeśli możliwe), networking web osobno.

---

## OQ-02 — Metoda logowania i minimum PII

**Status:** decided — [ADR 0007](decisions/0007-auth-providers-minimum-pii.md), [auth](architecture/auth.md)  
**Decyzja:** Google (Android/iOS/web) + Sign in with Apple na iOS; minimum PII (`profiles` bez email/imienia); zarys usuwania konta w auth.md. Poza MVP: magic link, anon+linkowanie, email+hasło.

---

## OQ-03 — Strategia offline / sync / konflikty

**Status:** decided — [ADR 0008](decisions/0008-sync-strategy-online-first.md), [data-and-sync](architecture/data-and-sync.md)  
**Decyzja:** online-first + lokalny cache odczytu; SoT = Postgres; zapis tylko online w MVP; konflikty = LWW na wiersz (`updated_at`), jedna reguła dla wszystkich typów encji; Realtime opcjonalne; offline-write później.  
**Schemat (`updated_at`, RLS):** [spec pierwszej migracji](specs/first-migration-profiles-rls.md) (#6); apply = [#21](https://github.com/MarekBiolik/GymManager/issues/21).

---

## OQ-04 — Narzędzia monorepo i wersjonowanie

**Status:** decided — [ADR 0005](decisions/0005-monorepo-tooling.md), [overview](architecture/overview.md)  
**Decyzja (skrót):** Gradle multi-project dla `core` + `apps/android` (export frameworka dla iOS); `apps/web` na pnpm/Vite poza Gradle; orkiestracja = cienkie skrypty root (bez Nx/Turborepo); core source-linked w tym samym commicie, SemVer/publikacja później.

---

## OQ-05 — Hosting Supabase i backupy

**Status:** decided — [ADR 0004](decisions/0004-supabase-hosting.md), [data-and-sync](architecture/data-and-sync.md#hosting-i-backupy)  
**Decyzja:** Supabase Cloud Free na start → Pro gdy potrzeba auto-backupów / braku pauzy; jeden projekt `dev` (+ opcjonalnie local CLI); sekrety poza repo; na Free okresowe `db dump`.

---

## OQ-06 — Los artefaktów prototypu

**Status:** decided — [legacy-cleanup](conventions/legacy-cleanup.md)  
**Artefakty:** `web/`, `server/`, `data/gymmanager.db*` — **usunięte** z working tree (nie były trackowane w git). Eksport schematu SQLite pominięty.

---

## OQ-07 — Źródło danych żywnościowych

**Status:** open  
**Kontekst:** stary prototyp używał OpenFoodFacts. Czy wracamy do OFF, własnej bazy produktów użytkownika only, czy hybryda?

**Zamyka:** przed fazą 3 (moduł żywienia).

---

## OQ-08 — UI/UX przełączania modułów

**Status:** open  
**Kontekst:** jedna aplikacja, kilka obszarów. Nawigacja (tabs, drawer, „mode switch”) — detal na fazę 6; nie blokuje fundamentów.

---

## Jak dodawać nowe pytania

1. Nadaj ID `OQ-NN`.
2. Status `open`, krótki kontekst, opcje jeśli znane, faza zamknięcia.
3. Po decyzji: status `decided` + link do ADR lub sekcji docs.
