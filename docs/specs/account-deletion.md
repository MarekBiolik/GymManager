# Spec: usuwanie konta (CASCADE + Edge Function)

**Status:** SPEC ONLY — **bez** kodu aplikacji, **bez** deploy Edge Function, **bez** zmian migracji SQL w tym issue.  
**Issue:** [#13](https://github.com/MarekBiolik/GymManager/issues/13)  
**Zarys behawioralny:** [auth — Usuwanie konta](../architecture/auth.md#usuwanie-konta)  
**Decyzja PII / Auth:** [ADR 0007](../decisions/0007-auth-providers-minimum-pii.md)  
**Schemat FK / RLS:** [pierwsza migracja](first-migration-profiles-rls.md) ([#6](https://github.com/MarekBiolik/GymManager/issues/6); apply = [#21](https://github.com/MarekBiolik/GymManager/issues/21))

Ten dokument jest **źródłem prawdy** dla mechanizmu kasowania konta w fazie 2. Implementacja (UI + Edge Function + smoke) idzie osobnym issue po apply migracji.

## Cele

1. Wybrać mechanizm: **FK `ON DELETE CASCADE` + zaufana Edge Function (`service_role`)**, z uzasadnieniem (nie „albo–albo”).
2. Ustalić kolejność: dane aplikacji znikają wraz z usunięciem Auth; sesje unieważnione.
3. Zapisać wymagania store (ścieżka **w apce**).
4. Opisać implikacje RLS względem pierwszej migracji (#6).

## Non-goals

| Poza zakresem | Gdzie |
|---------------|--------|
| Kod Edge Function / klientów | faza 2 (implementacja) |
| Zmiana draft SQL #6 | [#6](https://github.com/MarekBiolik/GymManager/issues/6) / [#21](https://github.com/MarekBiolik/GymManager/issues/21) — CASCADE już w specu |
| Soft-delete / tombstone | poza MVP ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |
| Usuwanie konta „tylko emailem do supportu” | sprzeczne z wymaganiami store |
| Kasowanie z klienta przez `anon` Admin API | **zakaz** ([auth — Role i klucze](../architecture/auth.md#role-i-klucze)) |

## Decyzja mechanizmu

| Warstwa | Rola | Decyzja |
|---------|------|---------|
| Schema | Sprzątanie wierszy aplikacji | FK `ON DELETE CASCADE` z tabel użytkownika / `profiles` → `auth.users(id)` — ustalone w [#6](https://github.com/MarekBiolik/GymManager/issues/6) |
| Orchestracja | Kto może skasować tożsamość Auth | **Edge Function** (lub równoważny zaufany backend) z **`service_role`** + Admin API `deleteUser` |
| Klient | UX | Ustawienia → potwierdzenie → wywołanie Function z JWT użytkownika; potem wylogowanie lokalne / czyszczenie cache |

**CASCADE i Edge Function nie są alternatywami** — uzupełniają się:

1. **Sam CASCADE** nie wystarczy jako produkt: klient z kluczem `anon` **nie** może usunąć wiersza w `auth.users`. Bez zaufanej ścieżki zostaje tożsamość Auth (email w IdP/Auth = PII).
2. **Sama Edge Function kasująca wiersze `public.*` pod `service_role` bez CASCADE** jest krucha: każda nowa tabela wymaga aktualizacji listy DELETE; łatwo o orphaned rows. CASCADE jest niezmiennikiem schematu.
3. **Kasowanie tylko przez RLS `DELETE` z klienta** usuwa dane apki, ale **nie** usuwa `auth.users` — konto wraca przy kolejnym OAuth; niespełnione minimum PII / store.

**Wybrane:** Function weryfikuje JWT → Admin `deleteUser(uid)` → Postgres usuwa zależne wiersze przez CASCADE → sesje Auth giną z użytkownikiem.

### Alternatywy odrzucone

| Opcja | Dlaczego nie |
|-------|----------------|
| Tylko CASCADE (bez Function) | Brak bezpiecznego triggera usunięcia Auth z apki |
| Tylko Function + ręczne DELETE wszystkich tabel | Dryf względem schematu; dubluje CASCADE |
| Database webhook / trigger na `profiles` → kasuj Auth | Odwrócona zależność; Auth Admin i tak potrzebuje `service_role`; trudniejszy audit |
| Soft-delete + anonimizacja | Poza MVP; store i tak oczekuje realnego usunięcia konta |

## Kolejność kasowania

Semantyka wymagana przez issue: **dane aplikacji → Auth; sesje unieważnione**.

Przy CASCADE realizujemy to **jednym wywołaniem Admin** (efekt atomowy z punktu widzenia produktu):

```mermaid
sequenceDiagram
  participant U as User
  participant App as Client
  participant EF as Edge Function
  participant Auth as Supabase Auth Admin
  participant PG as Postgres CASCADE
  U->>App: Usuń konto + potwierdzenie
  App->>EF: POST + JWT (Authorization)
  EF->>EF: verify JWT; uid = claim sub
  EF->>Auth: deleteUser(uid) service_role
  Auth->>PG: DELETE auth.users
  PG-->>Auth: CASCADE profiles + user_id rows
  Auth-->>EF: OK (sesje unieważnione)
  EF-->>App: 200
  App->>App: clear session + local cache
```

| Krok | Co | Uwagi |
|------|-----|--------|
| 1 | UX potwierdzenia | Świadome; trudne do przypadkowego kliknięcia (patrz niżej) |
| 2 | Edge Function | Tylko `uid` z **zweryfikowanego** JWT; **zakaz** kasowania cudzego `uid` z body bez sprawdzenia |
| 3 | `auth.admin.deleteUser(uid)` | Usuwa tożsamość; odświeżanie tokenów / sesje przestają działać |
| 4 | CASCADE | `profiles`, `measurements`, przyszłe tabele z FK → `auth.users` |
| 5 | Klient | Czyści lokalny JWT/cache; UI stanu wylogowanego |

**Nie** wymagamy osobnego masowego `DELETE` z klienta przed krokiem 3 — CASCADE gwarantuje brak orphanów przy poprawnych FK. Opcjonalny pre-delete pod RLS jest zbędny w MVP i utrudnia rollback przy błędzie sieci przed Auth delete.

Jeśli Admin `deleteUser` się **nie** powiedzie: dane apki zostają; użytkownik może ponowić. Jeśli `deleteUser` OK, a klient nie wyczyści cache: lokalne dane są stale — przy następnym starcie JWT jest nieważny → wymuszony logout (online-first, [ADR 0008](../decisions/0008-sync-strategy-online-first.md)).

## UX potwierdzenia

Poziom zachowania (bez layoutu):

1. Wejście tylko ze **ustawień konta** (nie z deep linka „jednym tapem”).
2. Jawny copy: usunięcie jest **trwałe**; sync i historia znikają; ponowne logowanie = **nowe** puste konto.
3. Potwierdzenie: drugi krok (np. wpisanie frazy / osobny dialog „Usuń na zawsze”) — nie pojedynczy toggle.
4. Po sukcesie: natychmiastowy stan wylogowany na tym urządzeniu; inne urządzenia tracą sesję przy kolejnym odświeżeniu tokena.

## Wymagania store (in-app deletion)

| Store | Wymaganie (kierunek) | Jak spełniamy |
|-------|----------------------|---------------|
| **Apple App Store** | Konto utworzone w apce musi dać się usunąć **w apce** (nie tylko „napisz do supportu”); proces łatwy do znalezienia | Ustawienia → Usuń konto → potwierdzenie → Function |
| **Google Play** | Ścieżka usunięcia konta w produkcie / widoczna w apce gdy oferujemy konta | Ta sama ścieżka na Androidzie |

Dodatkowo (produkt, nie osobny issue):

- Link do polityki prywatności / usuwania danych może istnieć w store listing — **nie zastępuje** in-app flow.
- Sign in with Apple / Google: usuwamy konto **GymManager** (Supabase Auth + dane); unlinking u IdP leży po stronie użytkownika w ustawieniach Google/Apple (poza zakresem apki).

## Relacja do #6 i RLS

### Pierwsza migracja ([spec](first-migration-profiles-rls.md))

| Element #6 | Rola w usuwaniu konta |
|------------|----------------------|
| `profiles.id` → `auth.users` **ON DELETE CASCADE** | Profil znika z Auth delete |
| `measurements.user_id` → `auth.users` **ON DELETE CASCADE** | Smoke + wzorzec dla faz 3–5 |
| Trigger `handle_new_user` | Tworzy profil; **nie** uczestniczy w delete |
| Hard delete (brak soft-delete) | Spójne z pełnym usunięciem konta |

**Zasada na przyszłe tabele:** każdy wiersz użytkownika ma FK do `auth.users(id)` (lub do `profiles.id` z CASCADE) z **`ON DELETE CASCADE`**, migracja razem z RLS — inaczej Function musiałaby znać listę tabel.

### Implikacje RLS

| Fakt | Konsekwencja |
|------|----------------|
| Polityki `DELETE` own row (#6) | Użytkownik **może** kasować własne wiersze; to **nie** jest ścieżka usuwania konta |
| RLS **nie** chroni `auth.users` przed Admin API | Dlatego Function używa `service_role` **tylko** po weryfikacji JWT |
| `service_role` omija RLS | Function **nie** powinna masowo skanować cudzych tabel „na zapas”; wystarczy `deleteUser` + CASCADE |
| Klucz `anon` w kliencie | Może wywołać Function z JWT; **nigdy** nie embedujemy `service_role` |

Szkic kontraktu Function (pseudokod — **nie** do commitowania jako kod w tym PR):

```text
POST /functions/v1/delete-account
Authorization: Bearer <user_access_token>

1. Validate JWT (Supabase); reject if missing/invalid
2. uid := jwt.sub
3. admin.deleteUser(uid)   // service_role; never trust body.uid alone
4. return 200
// App rows: CASCADE from auth.users — no manual DELETE list
```

Opcjonalnie później: rate limit / audyt log (bez PII w logach poza `uid`).

## Checklist implementacji (faza 2 — poza tym PR)

- [ ] Edge Function `delete-account` (lub równoważna nazwa) + secret `service_role` tylko w Supabase secrets
- [ ] UI ustawień na iOS / Android / web — ta sama semantyka
- [ ] Smoke: użytkownik testowy → delete → brak wierszy `profiles`/`measurements`; ponowne OAuth = nowy `auth.users` + pusty profil
- [ ] Dokumentacja sekretów / CI bez wycieku `service_role` ([ADR 0004](../decisions/0004-supabase-hosting.md))

## Powiązane

- [auth.md#usuwanie-konta](../architecture/auth.md#usuwanie-konta)
- [ADR 0007](../decisions/0007-auth-providers-minimum-pii.md)
- [first-migration-profiles-rls](first-migration-profiles-rls.md) (#6)
- [postgres-invariants](../architecture/postgres-invariants.md)
- [data-and-sync — RLS](../architecture/data-and-sync.md#row-level-security-rls)
- Roadmap faza 2
