# Spec: split środowisk Supabase (`staging` / `prod`)

**Status:** SPEC ONLY — **nie** tworzy projektów w Dashboard ani jobów CD w tym ticketcie.  
**Issue:** [#10](https://github.com/MarekBiolik/GymManager/issues/10)  
**Fundament:** [ADR 0004](../decisions/0004-supabase-hosting.md) (Cloud Free → Pro; na start jeden projekt `dev`)  
**Implementacja (później):** provision projektów + GitHub Environments + apply migracji — osobne ticketty po akceptacji tego specu; nie blokuje scaffoldu fazy 1 ([#14](https://github.com/MarekBiolik/GymManager/issues/14)).

Ten dokument zamyka follow-up ADR 0004: **kiedy** rozdzielić środowiska, **jak** je nazwać, **gdzie** trzymać URL/sekrety oraz **kolejność** aplikacji migracji. Bez kodu i bez klikania w Dashboard w ramach #10.

## Cele

1. Model `staging` + `prod` (osobne projekty Cloud) gotowy do wykonania przed publiczną betą.
2. Sekrety i URL **per środowisko** (GitHub Environments lub równoważne).
3. Jedyna dozwolona kolejność migracji: **staging → prod**.
4. Kryteria upgrade **`prod` → Pro** (backupy, pauza Free).

## Non-goals

| Poza zakresem | Gdzie |
|---------------|-------|
| Tworzenie drugiego/trzeciego projektu w Dashboard | osobny ticket implementacyjny po tym specu |
| Runbook dump/restore | [#9](https://github.com/MarekBiolik/GymManager/issues/9) |
| Scaffold monorepo / `supabase/` day one | [#14](https://github.com/MarekBiolik/GymManager/issues/14); start = jeden Cloud `dev` |
| Automatyczny CD apply na prod przy każdym merge | kierunek w [ci-cd](../conventions/ci-cd.md); włączenie jobów później |
| PITR / RPO < 1 dzień | ADR 0004 — gdy wymaganie pojawi się osobno |
| Self-host / wyjście z Cloud | exit path w ADR 0004 / data-and-sync |

## Kiedy split (trigger)

Zostajemy przy **jednym** projekcie Cloud `dev` (ADR 0004), dopóki spełnione są **wszystkie**:

- Solo / zaufany krąg; brak zewnętrznych beta-testerów z danymi „nie do utraty”.
- Brak potrzeby izolacji schematu „prawie prod” od osobistego dumpa developera.
- Limit Free (2 aktywne projekty) nie jest jeszcze problemem, bo używamy jednego slota Cloud.

**Wykonaj split przed** (wystarczy **pierwszy** z poniższych):

1. **Publiczna beta** albo zaproszenie użytkowników spoza maintainerów.
2. Dane w Cloud, których nie wolno mieszać z eksperymentami migracji / seedami deweloperskimi.
3. Potrzeba osobnego smoke środowiska przed apply na prawdziwych użytkownikach.

Split **nie** jest wymagany do zamknięcia fazy 1 ani „hello sync” na `dev`.

## Model docelowy (po splocie)

```text
local (opcjonalnie)     staging (Cloud)              prod (Cloud)
supabase start     →    schema + testowe dane   →    użytkownicy beta/prod
                        Free OK na start             Pro przed realnymi danymi*
```

\* Kryteria Pro: sekcja poniżej.

| Środowisko | Rola | Plan (kierunek) |
|------------|------|-----------------|
| `local` | Offline praca nad migracjami (`supabase start`); nie jest SoT sync użytkowników | CLI / Docker |
| `dev` (pre-split) | Jedyny Cloud do scaffoldu i osobistego użycia | Free |
| `staging` (post-split) | Integracja, apply migracji, smoke Auth/RLS, ewentualne testowe konta | Free OK |
| `prod` (post-split) | Dane prawdziwych użytkowników / bety | **Pro** wg kryteriów poniżej |

### Co z projektem `dev` przy splocie

Jedna z dwóch ścieżek (wybór w ticketcie implementacyjnym; obie OK względem tego specu):

1. **Rename / repurpose:** istniejący Cloud `dev` → `staging` (zachowaj historię migracji; wyczyść dane testowe jeśli trzeba). Utwórz nowy pusty `prod`.
2. **Fresh pair:** utwórz `staging` + `prod` od zera; odtwórz schemat z `supabase/migrations/`; stary `dev` **pauzuj lub usuń**, żeby zmieścić się w limicie aktywnych projektów Free (max 2), albo trzymaj `dev` tylko jeśli `prod` jest już na Pro i budżet/sloty na to pozwalają.

Po splocie **nie** utrzymujemy trzech aktywnych Free Cloud jako normy. Day-to-day developera: `local` + `staging`; `prod` tylko przez promocję migracji.

## Naming

| Warstwa | Konwencja | Przykład |
|---------|-----------|----------|
| Projekt Supabase (Dashboard name) | `gymmanager-<env>` | `gymmanager-staging`, `gymmanager-prod` |
| Ref / id projektu | z Dashboard (losowy); **nie** hardcodować w docs | `abcdefghijkl` |
| GitHub Environment | `staging`, `production` | Settings → Environments |
| Pliki env lokalne (nigdy w git) | `.env.staging`, `.env.production` / `.env.local` | gitignore już na `master` |
| Prefiks sekretów CI (opcjonalny) | bez prefixu **wewnątrz** Environment (izolacja robi Environment) | `SUPABASE_URL` w env `staging` ≠ ten w `production` |

Unikać nazw `dev`/`prod` mylonych z branchami git. Branch = feature/docs; **środowisko = projekt Cloud + GitHub Environment**.

## Sekrety i URL per środowisko

Źródło prawdy po splocie: **GitHub Environments** (`staging`, `production`) — nie jeden płaski zestaw Secrets na całe repo dla URL Cloud.

### Zmienne / sekrety (powtórzone w każdym Environment)

| Nazwa | Typ | Do czego | Gdzie wolno |
|-------|-----|----------|-------------|
| `SUPABASE_URL` | secret lub variable | Klienci, CI smoke, link dokumentacyjny lokalny | Environment; lokalny `.env.*` |
| `SUPABASE_ANON_KEY` | secret | Klienci (ochrona = RLS) | Environment; lokalny `.env.*`; **nie** w logach CI |
| `SUPABASE_SERVICE_ROLE_KEY` | secret | Tylko admin / migracje / zaufany job — **nigdy** klient | Environment (restricted); lokalny admin `.env*` |
| `SUPABASE_PROJECT_REF` | variable / secret | CLI `link` / `db push` | Environment |
| `SUPABASE_DB_PASSWORD` | secret | Gdy CI łączy się do DB poza access tokenem | Environment |
| `SUPABASE_ACCESS_TOKEN` | secret | Personal/access token CLI w CI (jeśli używamy) | Environment / org secret; rotacja |

Zasady (zgodne z ADR 0004):

1. Osobny zestaw wartości dla `staging` i `production` — **żadnego** współdzielenia URL/`anon` między środowiskami w buildach release.
2. `service_role` wyłącznie w Environment z wymaganiem review / ograniczonymi branchami (np. tylko `master` → job prod).
3. Klienci mobilne/web: build **staging** vs **prod** dostaje URL+`anon` z właściwego Environment (lub store track / flavor) — nie z hardcodu w repo.
4. W docs/PR: placeholdery (`YOUR_STAGING_URL`), nigdy prawdziwe klucze.
5. Równoważne poza GitHubem (np. inny CI): ta sama izolacja „per środowisko”; nazwa store'a może się różnić.

Do czasu splitu: jeden zestaw pod Cloud `dev` (jak dziś w ADR 0004 / playbooku).

## Kolejność aplikacji migracji

Źródło prawdy schematu: pliki w `supabase/migrations/` w git (po [#14](https://github.com/MarekBiolik/GymManager/issues/14) / [#21](https://github.com/MarekBiolik/GymManager/issues/21)). **Zakaz** trwałego dryfu przez klik w Dashboard SQL Editor na `staging`/`prod`.

```text
PR → merge do master
      → apply na staging
      → smoke (Auth/RLS / krytyczna ścieżka)
      → apply **tych samych** plików migracji na prod
```

| Krok | Środowisko | Reguła |
|------|------------|--------|
| 1. Draft / lokalnie | `local` lub efemeryczna baza | `supabase db reset` / lint; opcjonalnie |
| 2. Integracja | **`staging`** | Pierwszy Cloud apply po merge (lub controlled job) |
| 3. Weryfikacja | `staging` | Smoke; przy fail — **stop**, nie ruszaj `prod` |
| 4. Promocja | **`prod`** | Te same migracje co na staging; bez „hotfiksu tylko na prod” |
| 5. Rollback | — | Forward-fix preferowany; restore wg [#9](https://github.com/MarekBiolik/GymManager/issues/9) / Pro backups |

Dodatkowe twarde reguły:

- **Nigdy** `prod` przed `staging` dla tej samej migracji.
- **Nigdy** ręczna zmiana schematu tylko na `prod`.
- Dane **nie** są kopiowane automatycznie staging → prod (osobne projekty; Auth users też osobno). Seed staging ≠ prod.
- Pre-split: apply tylko na `dev` (jedyny Cloud); po splocie `dev` znika z ścieżki promocji.

## Kryteria upgrade `prod` → Pro

Upgrade **`prod`** do Pro, gdy zachodzi **którykolwiek** punkt (nie czekać na katastrofę):

| Trigger | Dlaczego |
|---------|----------|
| Publiczna beta / zewnętrzni użytkownicy z danymi | Free nie daje dashboardowych daily backupów; pauza po bezczynności jest nieakceptowalna dla „żywego” prod |
| Dane „nie do odtworzenia z pamięci / dumpa ad hoc” | Potrzebne dostępne daily backups (retencja 7 dni) |
| Ryzyko pauzy Free (~1 tydzień bezczynności) | Prod musi być dostępny bez ręcznego odpauzowania |
| Więcej DB/storage/Auth niż Free | Limit zasobów |

**Kiedy nie trzeba Pro od razu na `staging`:** staging może zostać na Free (ew. pauza OK dla maintainerów). **`prod` upgrade’ujemy wcześniej niż staging.**

PITR (add-on): dopiero gdy RPO < ~1 dzień stanie się wymaganiem (ADR 0004) — poza tym specem.

**Świadoma decyzja kosztowa:** sam staging Free + prod Pro jest domyślnym kształtem po publicznej becie; dwa Pro tylko gdy staging też wymaga non-stop uptime / auto-backupów.

## Checklist implementacyjna (poza #10)

Do osobnego ticketu / PR kod+ops (nie ten dokument):

- [ ] Utworzyć projekty `gymmanager-staging` / `gymmanager-prod` (lub rename `dev` → staging)
- [ ] GitHub Environments `staging` + `production` z tabelą sekretów powyżej
- [ ] Podpiąć CLI / CI do `PROJECT_REF` per Environment
- [ ] Zastosować pełny łańcuch migracji na staging, potem pusty prod
- [ ] Upgrade `prod` → Pro wg kryteriów (przed realnymi użytkownikami)
- [ ] Zaktualizować build flavors / env web pod dwa URL
- [ ] Oznaczyć stary `dev` jako retired (pause/delete) jeśli nie jest już `staging`

## Powiązane

- [ADR 0004](../decisions/0004-supabase-hosting.md) — hosting, Free→Pro, sekrety baseline, deferral tego splitu
- [data-and-sync — Hosting i backupy](../architecture/data-and-sync.md#hosting-i-backupy)
- [ci-cd](../conventions/ci-cd.md)
- [phase-1-scaffold-playbook](../plans/phase-1-scaffold-playbook.md) — day one nadal tylko `dev`
- [#9](https://github.com/MarekBiolik/GymManager/issues/9) dump/restore
