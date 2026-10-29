# Dane i synchronizacja

## Source of truth

**PostgreSQL (Supabase)** jest źródłem prawdy dla danych użytkownika po zsynchronizowaniu. Lokalny cache na urządzeniu jest optymalizacją UX (offline / szybkość), nie drugą „prawdą” — szczegóły strategii: [OQ-03](../open-questions.md).

## Zarys schematu

Pełniejszy model domenowy: [data-model](../domain/data-model.md). Na poziomie bazy (kierunek):

| Obszar | Przykładowe tabele | Uwagi |
|--------|-------------------|--------|
| Tożsamość | `profiles` (opcjonalnie, 1:1 z `auth.users`) | Minimum PII |
| Żywienie | `foods`, `food_entries` | Własne produkty + wpisy spożycia |
| Trening | `exercises`, `workouts` / `sessions`, `sets` | Historia sesji |
| Pomiary | `measurements` | Waga i inne metryki w czasie |
| Wspólne | `tags` (opcjonalnie) | Etykiety cross-cutting |

Wszystkie tabele użytkownika mają `user_id` (UUID → `auth.users`) i polityki **RLS**: użytkownik widzi/edytuje tylko swoje wiersze.

## Row Level Security (RLS)

``mermaid
flowchart LR
  Client["Klient + JWT"]
  API["Supabase API"]
  RLS["RLS policies"]
  DB["Postgres rows"]
  Client --> API --> RLS --> DB
``

- Klucz `anon` w kliencie jest OK — bezpieczeństwo opiera się na RLS + JWT, nie na ukrywaniu URL.
- Klucz `service_role` **nigdy** w aplikacjach klienckich ani w publicznym repo.
- Nowe tabele: migracja SQL **razem** z politykami RLS (konwencja CI).

## Sync (zarys — do doprecyzowania w OQ-03)

Kierunki do rozstrzygnięcia:

1. **Online-only na start** — prostsze; offline później.
2. **Local-first + kolejka** — lepsze UX w siłowni bez zasięgu; więcej złożoności (konflikty, zegary).
3. **Realtime Supabase** — live updates między urządzeniami gdy online.

Rekomendacja robocza do walidacji w fazie 2: zacząć od solidnego online + prosty lokalny cache odczytu; pełne offline-write dopiero gdy MVP modułów tego wymaga.

### Konflikty (gdy offline-write)

Kandydaci: last-write-wins na wiersz; merge per typ encji; „server wins”. Wybór per typ danych (wpis posiłku ≠ szablon ćwiczenia).

## Agregacje i historia

Tracker z rokiem+ historii (kalorie/dzień, trendy wagi, objętość treningu) naturalnie korzysta z **SQL** (widoki, `GROUP BY`, okna). To jeden z powodów wyboru Postgresa ([ADR 0002](../decisions/0002-backend-supabase.md)).

Agregacje ciężkie można materializować później (np. codzienne sumy) — nie w iteracji 1.

## Storage

Pliki (np. zdjęcia opakowań / postęp) → Supabase Storage + RLS na bucketach. Na razie opcjonalne; model plików dopiszemy przy potrzebie modułu.

## Migracje

- Katalog `supabase/migrations/` (faza 1).
- Każda zmiana schematu = migracja w PR.
- Zakaz „ręcznej” zmiany prod bez migracji.

## Hosting i backupy

Decyzja: [ADR 0004](../decisions/0004-supabase-hosting.md) (zamyka [OQ-05](../open-questions.md)).

### Hosting

| Element | Na start | Upgrade path |
|---------|----------|--------------|
| Platforma | **Supabase Cloud** | — |
| Plan | **Free** | **Pro** gdy: potrzebujemy dostępnych daily backupów, braku pauzy projektu, więcej DB/storage, albo dane „nie do odtworzenia z pamięci” |
| Self-host | Nie | Opcja exit (Postgres dump → inny hosting), nie cel operacyjny |

### Środowiska

| Środowisko | Na start | Uwagi |
|------------|----------|--------|
| `dev` | **Jeden** projekt Cloud | Scaffolding, migracje, osobiste użycie trackera |
| `local` | Opcjonalnie `supabase start` | Praca nad schematem offline; nie zastępuje `dev` jako źródła prawdy sync |
| `staging` / `prod` | **Później** | Osobne projekty przy publicznej becie / wielu użytkownikach; prod zwykle na Pro |

Free: max 2 aktywne projekty; pauza po ~1 tygodniu bezczynności — odpauzować przed pracą.

### Sekrety

| Sekret | Gdzie wolno | Gdzie zakaz |
|--------|-------------|-------------|
| Project URL + `anon` | Klienci, lokalny `.env*` | Commitowanie nie jest „wyciekiem RLS”, ale **nie** commitujemy `.env*` — URL/klucze per środowisko trzymamy poza gitem |
| `service_role` | Wyłącznie lokalny admin / przyszły zaufany backend; GitHub Secrets w CI jeśli kiedyś potrzebne | Aplikacje klienckie, publiczne repo, logi CI |
| OAuth client secrets (Google/Apple) | Dashboard Supabase Auth + (gdy trzeba) GitHub Secrets | Repo, klienci |

Zasady:

1. `.env`, `.env.local`, `.env.*.local` — w `.gitignore` (już na `master`).
2. W dokumentacji i PR-ach: placeholdery (`YOUR_PROJECT_URL`), nigdy prawdziwe wartości.
3. CI: GitHub Secrets; żadnych sekretów w logach builda — [ci-cd](../conventions/ci-cd.md).

### Polityka backupów / restore

**Free (obecny plan):**

- Dashboard **nie udostępnia** automatycznych daily backupów do restore (to Pro+).
- **Okresowy dump logiczy** poza Cloud: `supabase db dump` (lub równoważny `pg_dump`) — minimum przed ryzykowaną migracją i regularnie gdy w `dev` są dane warte odzyskania.
- Dump + pliki migracji w git = ścieżka odtworzenia schematu; dump z danymi = odtworzenie zawartości.
- Po pauzie Free: odpauzowanie w dashboardzie; dane projektu pozostają, ale bez własnych dumpów nie mamy niezależnej kopii.

**Po upgrade do Pro:**

- Daily backups (retencja 7 dni) w Dashboard → Database → Backups.
- PITR (add-on) dopiero gdy RPO < ~1 dzień stanie się wymaganiem.

**Restore (poziom scaffold):**

1. Preferuj odtworzenie schematem z `supabase/migrations/` + ponowne załadowanie danych z dumpa.
2. Na Pro: restore z daily backup w dashboardzie według dokumentacji Supabase.
3. Drill restore (ćwiczenie na pustym projekcie) — follow-up [#9](https://github.com/MarekBiolik/GymManager/issues/9); nie blokuje decyzji hostingu.
4. Split `staging` / `prod` — follow-up [#10](https://github.com/MarekBiolik/GymManager/issues/10) przed publiczną betą.

### Exit path

Dane w standardowym Postgresie → eksport SQL / dump → inny Postgres lub self-host Supabase. Auth/API Supabase to warstwa do przepisania przy pełnym wyjściu; sam model danych nie jest zamknięty w proprietarnym store.
