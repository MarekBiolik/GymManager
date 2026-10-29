# Roadmapa

Fazy pracy. Każda faza kończy się aktualizacją dokumentacji (lub ADR), zanim idzie głęboko w kod.

## Faza 0 — Fundamenty dokumentacji (obecna)

**Cel:** zarys wizji, architektury, domeny, konwencji i listy otwartych pytań.

**Output:** katalog `docs/` (ten zestaw dokumentów).

**Done when:** ADR-y 0001–0003 przyjęte; open-questions wypisane; konwencje bazowe istnieją.

---

## Faza 1 — Scaffolding monorepo i infrastruktury

**Cel:** puste, budujące się projekty + podpięty Supabase (dev) + CI szkielet.

Zakres (do precyzowania przed startem):

- Struktura wg [ADR 0005](decisions/0005-monorepo-tooling.md): Gradle (`core`, `apps/android`), Xcode (`apps/ios`), pnpm (`apps/web`), `supabase/`, root scripts
- Pierwsza migracja schematu (minimalna: `profiles` + 1–2 tabele domenowe)
- Auth smoke-test (Google; Apple na iOS gdy App Store)
- Podpięcie projektu Supabase Cloud (`dev`) wg [ADR 0004](decisions/0004-supabase-hosting.md); pierwsze sekrety lokalne + checklista dumpów
- Playbook kolejności scaffoldu (issue #7) — docs przed tworzeniem folderów kodu
- Web bez KMP→JS/Wasm w scaffoldzie ([ADR 0006](decisions/0006-web-supabase-js-without-kmp-js.md) — `supabase-js`)

**Zależne open questions:** (brak blokujących OQ-01/04/05).  
**Zamknięte przed scaffoldem:** hosting Supabase ([OQ-05](open-questions.md) → ADR 0004); monorepo ([OQ-04](open-questions.md) → ADR 0005); interop web↔core ([OQ-01](open-questions.md) → ADR 0006).

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
