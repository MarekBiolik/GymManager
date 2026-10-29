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

**Zależne open questions:** interop web↔core (OQ-01).  
**Zamknięte przed scaffoldem:** hosting Supabase ([OQ-05](open-questions.md) → ADR 0004); monorepo ([OQ-04](open-questions.md) → ADR 0005).

---

## Faza 2 — Auth + sync baseline

**Cel:** użytkownik loguje się, dane są izolowane RLS, podstawowy sync działa na wszystkich klientach.

Zakres:

- Finalna metoda logowania (PII minimum)
- Strategia offline/cache i konflikty
- Shared repositories w core + cienkie UI „hello sync”

**Zależne open questions:** auth method, offline/sync, web↔KMP interop.

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
