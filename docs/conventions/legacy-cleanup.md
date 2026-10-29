# Los artefaktów prototypu

## Stan faktyczny (iteracja 1)

W repozytorium git na `master` jest zacommitowany praktycznie tylko `.gitignore`. Na dysku roboczym pozostały **nieźródłowe** resztki starego prototypu (tracker żywienia: React/Vite + Node + SQLite + OpenFoodFacts):

| Ścieżka | Co to jest | Decyzja |
|---------|------------|---------|
| `web/dist/` | Zbudowany frontend (HTML/JS/CSS) | **Usunąć** przy sprzątaniu fazy 1; nie commitować |
| `web/node_modules/` | Zależności npm bez `package.json` w drzewie źródeł | **Usunąć**; nie commitować |
| `web/src/` | Pusty / bez plików źródeł | **Usunąć** katalog razem ze starym `web/` albo zastąpić nowym `apps/web/` |
| `server/` | Katalog bez źródeł | **Usunąć** |
| `data/gymmanager.db` (+ `-wal`, `-shm`) | Lokalna baza SQLite prototypu | **Zachować tymczasowo** poza gitem jako referencję; przed usunięciem: opcjonalny eksport schematu/danych do `docs/legacy/` lub pliku SQL; potem skasować |
| `.gitignore` | Istniejący | **Zachować i uzupełnić** pod monorepo (faza 1) |

## Zasady

1. Nic z powyższych artefaktów **nie wchodzi** do historii git jako „kod produkcyjny”.
2. Nowy web żyje w `apps/web/`, nie w starym `web/`.
3. SQLite nie jest backendem docelowym — docelowo Postgres (Supabase).
4. OpenFoodFacts może wrócić jako integracja — osobna decyzja ([OQ-07](../open-questions.md)), nie przez wskrzeszanie starego serwera.

## Checklist wykonania

- [x] `.gitignore` pokrywa `node_modules/`, `dist/`, `*.db`, `.env*` (już na `master`)
- [ ] Opcjonalnie: zrzut schematu `gymmanager.db` → `docs/legacy/prototype-schema.sql` (pominięte — baza usunięta bez eksportu)
- [x] Usunięto `web/` (w tym `dist`, `node_modules`, puste `src`), `server/`, `data/gymmanager.db*`
- [x] Sprzątanie lokalne wykonane przy porządkowaniu brancha po iteracji docs

## Status OQ

[OQ-06](../open-questions.md) — **decided**; artefakty prototypu usunięte z working tree (nie były w git).
