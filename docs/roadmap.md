# Roadmapa

Fazy pracy. Każda faza kończy się aktualizacją dokumentacji (lub ADR), zanim idzie głęboko w kod.

## Faza 0 — Fundamenty dokumentacji

**Cel:** zarys wizji, architektury, domeny, konwencji i listy otwartych pytań.

**Output:** katalog `docs/` (ten zestaw dokumentów).

**Status:** done — ADR 0001–0003 (+ późniejsze 0004–0007 w iteracjach OQ); open-questions i konwencje bazowe istnieją.

---

## Faza 1 — Scaffolding monorepo i infrastruktury

**Cel:** puste, budujące się projekty + podpięty Supabase (`dev`) + CI szkielet.

**Playbook (kolejność, done per krok, non-goals):** [plans/phase-1-scaffold-playbook.md](plans/phase-1-scaffold-playbook.md) — zamyka [#7](https://github.com/MarekBiolik/GymManager/issues/7).  
**Implementacja kodu:** [#14](https://github.com/MarekBiolik/GymManager/issues/14) (nie ten dokument).

Zakres:

- Struktura wg [ADR 0005](decisions/0005-monorepo-tooling.md): Gradle (`core`, `apps/android`), Xcode (`apps/ios`), pnpm (`apps/web`), `supabase/`, root scripts
- Web wyłącznie `supabase-js` — **bez** KMP→JS/Wasm ([ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md))
- Podpięcie Supabase Cloud `dev` ([ADR 0004](decisions/0004-supabase-hosting.md)); sekrety lokalne / GitHub Secrets; dumpi → [#9](https://github.com/MarekBiolik/GymManager/issues/9); split `staging`/`prod` przed publiczną betą → [spec](specs/supabase-staging-prod-split.md) ([#10](https://github.com/MarekBiolik/GymManager/issues/10)) (nie w #14)
- Pierwsza migracja schematu: **spec** [first-migration-profiles-rls](specs/first-migration-profiles-rls.md) ([#6](https://github.com/MarekBiolik/GymManager/issues/6)); **apply** = [#21](https://github.com/MarekBiolik/GymManager/issues/21) (po OQ-03 → [ADR 0008](decisions/0008-sync-strategy-online-first.md))
- Szkielet CI na PR (Gradle core/android + pnpm web + testy core) — szczegóły w playbooku / [ci-cd](conventions/ci-cd.md)
- Auth: **nie** pełny smoke OAuth w scaffoldzie; IdP checklist [#12](https://github.com/MarekBiolik/GymManager/issues/12), login UI → faza 2

**Done when (przed startem głębokiego kodu fazy 2):**

1. Playbook zmergowany; [#14](https://github.com/MarekBiolik/GymManager/issues/14) zmergowane — drzewo buduje się lokalnie i na CI.
2. `supabase/` + link do Cloud `dev`; brak sekretów w git.
3. Spec [#6](https://github.com/MarekBiolik/GymManager/issues/6) / [docs/specs/first-migration-profiles-rls.md](specs/first-migration-profiles-rls.md) zmergowany i migracja SQL 1:1 ze specem w `supabase/migrations/` + na `dev` ([#21](https://github.com/MarekBiolik/GymManager/issues/21)).
4. Brak targetu JS/Wasm w `core`; web nie konsumuje binary KMP.
5. Follow-upy dump (#9) / IdP (#12) otwarte świadomie — nie blokują zamknięcia samego layoutu; #6+#21 blokują „faza 1 complete”.

**Zamknięte przed startem kodu scaffoldu:** OQ-05 → ADR 0004; OQ-04 → ADR 0005; OQ-01 → ADR 0006; OQ-03 → ADR 0008; playbook #7; spec migracji #6.  
**Nie blokuje startu pustego #14:** apply SQL (#21) — treść schematu jest w specu; #14 trzyma `migrations/` puste do #21.

---

## Faza 2 — Auth + sync baseline

**Cel:** użytkownik loguje się, dane są izolowane RLS, podstawowy sync działa na wszystkich klientach.

Zakres:

- Implementacja logowania wg [ADR 0007](decisions/0007-auth-providers-minimum-pii.md) / [auth](architecture/auth.md) (spec zamknięty; tu kod + konfiguracja IdP)
- Usuwanie konta (cascade / Edge Function) wg zarysu w auth.md
- Sync wg [ADR 0008](decisions/0008-sync-strategy-online-first.md): zapis online + cache odczytu + LWW; cienkie UI „hello sync”
- Shared repositories w core (mobile) + równoważne wołania `supabase-js` (web)

**Zależne open questions:** (brak blokujących OQ-02/03). Auth/PII: **decided** ([OQ-02](open-questions.md) → ADR 0007). Sync/offline: **decided** ([OQ-03](open-questions.md) → ADR 0008). Web nadal na `supabase-js` ([ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md)); parity offline-write z core dopiero przy revisit / osobnym ADR.

---

## Faza 3 — Moduł żywienia (MVP)

**Cel:** własne produkty/posiłki, wpisy spożycia, historia, podstawowe agregacje.

Może wykorzystać zewnętrzne źródło żywności (np. OpenFoodFacts) — decyzja osobnym ADR.

---

## Faza 4 — Moduł treningu (MVP)

**Cel:** ćwiczenia, sesje, serie, historia treningów.

---

## Faza 5 — Moduł pomiarów (MVP)

**Cel:** pomiary ciała w czasie, proste wykresy/agregacje (Postgres).

---

## Faza 6 — Integracja UX „jedna apka”

**Cel:** spójna nawigacja między modułami, polish, App Store / Play Store / hosting web.

---

## Faza 7 — AI (później)

**Cel:** skille / agenci / workflow wokół ustabilizowanej infrastruktury i domeny.

Świadomie **po** fazach 0–6. Nie projektujemy tu stacku AI.

---

## Jak pracujemy z roadmapą

1. Przed startem fazy: krótki plan + doprecyzowanie powiązanych punktów z [open-questions](open-questions.md).
2. Decyzje trwałe → ADR.
3. Kod dopiero po zaktualizowanej dokumentacji obszaru.
4. Roadmapa nie jest kontraktem dat — to kolejność zależności.
