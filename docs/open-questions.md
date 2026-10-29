# Otwarte pytania

Obszary świadomie **nie rozstrzygnięte** w iteracji 1. Każdy punkt powinien zostać zamknięty osobną iteracją (plan + ADR lub aktualizacja docs) przed głębokim kodowaniem.

Statusy: `open` | `in progress` | `decided` (z linkiem do ADR/docs).

---

## OQ-01 — Interop web ↔ wspólny core (KMP)

**Status:** open  
**Dlaczego ważne:** KMP naturalnie spina Android i iOS; web (JS/Wasm) jest trudniejszy. Złe rozwiązanie = duplikacja logiki domenowej albo kruchy interop.

**Opcje (zarys):**

1. Core skompilowany do JS/Wasm, konsumowany przez React/TS.
2. Web gada wprost do Supabase przez `supabase-js`; cienka logika w TS (ryzyko duplikacji względem core).
3. Hybryda: domena/walidacje w shared (jeśli możliwe), networking web osobno.

**Zamyka:** przed lub w fazie 1 (scaffolding) / najpóźniej fazie 2.

---

## OQ-02 — Metoda logowania i minimum PII

**Status:** open  
**Kontekst:** potrzebujemy sync między urządzeniami, bez zbierania zbędnych danych o użytkowniku. Google login jest wygodny i darmowy w Supabase Auth, ale przekazuje email.

**Opcje (zarys):** Google OAuth, Apple Sign In (wymagane przy Google na iOS w App Store), magic link, konto anonimowe + późniejsze linkowanie urządzeń.

**Zamyka:** faza 2; szkielet w [auth](architecture/auth.md).

---

## OQ-03 — Strategia offline / sync / konflikty

**Status:** open  
**Pytania:** czy zapis lokalny-first z późniejszym sync? Optimistic UI? Source of truth = Postgres? Jak rozwiązywać konflikty (last-write-wins vs merge per-encja)?

**Zamyka:** faza 2; szkielet w [data-and-sync](architecture/data-and-sync.md).

---

## OQ-04 — Narzędzia monorepo i wersjonowanie

**Status:** open  
**Pytania:** Gradle multi-project + osobne `apps/web` (npm/pnpm)? Czy jedno narzędzie orkiestracji (np. Nx / Turborepo tylko dla web + skrypty root)? Jak wersjonować `core` względem klientów?

**Zamyka:** faza 1.

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
