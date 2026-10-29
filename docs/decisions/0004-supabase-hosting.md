# ADR 0004 — Hosting Supabase i backupy

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja zamykania OQ-05)

## Kontekst

[ADR 0002](0002-backend-supabase.md) wybrał Supabase jako BaaS, ale nie rozstrzygnął **gdzie** go hostujemy ani jak chronimy dane. Solo maintainer trackera fitness: niski budżet utrzymania, brak opsów, dane osobiste (historia posiłków/treningów) warte odzyskania, ale na start nie jest to multi-tenant SaaS z SLA.

Siły: koszt (idealnie $0 na start), zero VPS/patchowania, exit path przez SQL, sensowna ścieżka upgrade gdy dane staną się „nie mogę stracić”, jasny model sekretów przed scaffoldingiem.

## Decyzja

1. **Hosting:** **Supabase Cloud**, plan **Free** na start. Upgrade do **Pro** (~$25/mies.) gdy potrzebujemy automatycznych daily backupów, braku pauzy projektu albo więcej zasobów — zanim dane produkcyjne będą nie do odtworzenia z pamięci.
2. **Środowiska:** na start jeden projekt Cloud = **`dev`** (scaffolding + osobiste użycie) + opcjonalnie lokalny stack (`supabase start`). Osobne **`staging` / `prod`** (osobne projekty Cloud) — **przed publiczną betą / zewnętrznymi użytkownikami**; model, naming, sekrety per Environment, kolejność migracji (`staging` → `prod`) i kryteria Pro na `prod`: [spec split środowisk](../specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)).
3. **Sekrety:** URL projektu + klucz `anon` mogą być w klientach (ochrona = RLS). `service_role` i inne sekrety **nigdy** w repo ani w apkach — tylko lokalny `.env*` (gitignore) i GitHub Secrets / **GitHub Environments** w CI (po splocie: osobny zestaw per `staging` / `production`).
4. **Backupy (Free):** brak dostępnych automatycznych backupów dashboardowych. Polityka: okresowy logiczny dump (`supabase db dump` / `pg_dump`) poza Cloud + dump przed ryzykowną migracją. Po upgrade do Pro: daily backups (7 dni) + opcjonalnie PITR. **`prod` → Pro** zanim realne dane bety (szczegóły w specu split).

## Alternatywy rozważone

1. **Supabase Cloud Free → Pro (wybrane)** — zero ops, darmowy start, oficjalne Auth/API, exit przez Postgres dump. Wady: pauza Free po ~1 tygodniu bezczynności; brak dostępnych auto-backupów na Free; limit 2 aktywnych projektów Free.
2. **Od razu Cloud Pro** — daily backups i brak pauzy od dnia 1. Odrzucone na start: niepotrzebny koszt, dopóki nie ma danych wartych płatnego RPO.
3. **Self-host na VPS (Docker)** — pełna kontrola, brak pauzy. Odrzucone na start: patching, TLS, backupy, upgrade’y stacku = ops, którego solo nie chcemy przed MVP.
4. **Dwa projekty Cloud od razu (dev + prod)** — czystszy podział. Odrzucone na start: Free ma limit aktywnych projektów; staging/prod wg [specu #10](../specs/supabase-staging-prod-split.md) przy publicznej becie (`prod` zwykle Pro).

## Konsekwencje

- Pozytywne: decyzja gotowa pod fazę 1; niski koszt; słabszy lock-in (SQL); jasna ścieżka upgrade i **udokumentowany** split środowisk (spec #10).
- Negatywne / koszty: na Free trzeba **samemu** dumpować; projekt Free może zostać wstrzymany przy bezczynności — po pauzie trzeba go odpauzować przed pracą; limit 2 projektów Free (po splocie: zwykle `staging` Free + `prod` Pro; bez trzech aktywnych Free).
- Doprecyzować później: checklista provisioningu przy scaffoldzie; drill restore ([#9](https://github.com/MarekBiolik/GymManager/issues/9)); wykonanie splitu wg specu (nie ten ADR); PITR gdy RPO < 1 dzień stanie się wymaganiem.
- Follow-upy: [#9 runbook dump/restore](https://github.com/MarekBiolik/GymManager/issues/9). Split staging/prod: **spec** [supabase-staging-prod-split](../specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)); implementacja projektów/Environments = osobny ticket.

## Powiązane

- [OQ-05](../open-questions.md) (decided)
- [data-and-sync](../architecture/data-and-sync.md) — sekcja Hosting i backupy
- [Spec: split staging/prod](../specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10))
- [auth](../architecture/auth.md) — klucze / sekrety
- [tech-stack](../architecture/tech-stack.md)
- [ci-cd](../conventions/ci-cd.md)
- [ADR 0002](0002-backend-supabase.md)
- [#9](https://github.com/MarekBiolik/GymManager/issues/9)
