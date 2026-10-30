# Spec: Nutrition module (phase 3)

**Status:** living contract for phase-3 implementation  
**Parent decisions:** [ADR 0011](../decisions/0011-food-data-hybrid-in-phase-3.md) (hybrid in phase 3; supersedes ADR 0009 phase boundary), [ADR 0008](../decisions/0008-sync-strategy-online-first.md), [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)  
**Domain:** [data-model](../domain/data-model.md), [glossary](../domain/glossary.md)  
**OFF hybrid detail:** [openfoodfacts-import-hybrid](openfoodfacts-import-hybrid.md)

Personal Fitatu-like nutrition: goals/settings, own foods + flexible servings, weekly diary with meal sections, OFF search/import + barcode, recipes, charts, water, copy meal/day — Android + web.

## Goals

1. Per-user nutrition goals (kcal + P/C/F), height (cm), water target (ml); weight via existing `measurements` (`body_weight_kg`).
2. Own `foods` catalog with macros **per 100 g**, flexible `food_servings` (e.g. portion / teaspoon → grams).
3. Weekly diary with editable meal types; add by name (own + OFF) and barcode; amount by grams or serving.
4. Goal-progress rings; recent/favorites ranking; recipes; charts; water; copy meal/day.
5. Hybrid: OFF → copy into own `foods`; diary never depends on live OFF.

## Non-goals

| Out of scope | Where |
|--------------|--------|
| Imperial units (lb/oz) | deferred; `preferred_units` may stay `kg` |
| Global / shared product catalog | ADR 0011 / OQ-10 seed only if needed |
| LRU auto-delete of foods | ranking only |
| Offline write queue | [offline-write-queue](offline-write-queue.md) |
| iOS product UI beyond build-spec | until Mac access (#49) |
| Cross-module nav polish | phase 6 / [OQ-08](../open-questions.md) |

## Units (MVP)

- Mass: **g** / **kg** only. Height: **cm**. Water: **ml**. Energy: **kcal**.
- Canonical food macros: **per 100 g**. Entry stores resolved `grams` + macro snapshot.

## Entities (summary)

See [data-model](../domain/data-model.md) for columns. Outline:

| Entity | Role |
|--------|------|
| `nutrition_settings` | 1:1 user goals + height + water target |
| `meal_types` | Per-user ordered sections (seed: breakfast, 2nd breakfast, lunch, snack, dinner) |
| `foods` | Own products/recipes; optional `external_*`; `last_used_at`; `is_favorite` |
| `food_servings` | Label + grams per food |
| `food_entries` | Diary row: date, meal type, amount/unit, grams, macro snapshot |
| `recipe_ingredients` | Recipe food → component food + grams |
| `water_entries` | ml per day (or per drink event — implementation: per-day total or add events; prefer addable events summed per day) |

## Flows

### Diary day

1. User opens week strip → selects day.
2. Sees meal sections with entries + daily kcal/P/C/F vs goals (progress rings).
3. Add → search own foods (recent/favorites first) → optional OFF search → optional barcode → pick amount (g or serving) → save entry (snapshot macros; bump `last_used_at`).

### Settings

1. Edit kcal + protein/carbs/fat targets, height_cm, water_target_ml.
2. Reorder / rename / add / remove meal types (must leave ≥1).
3. Log weight via measurements UI (or thin weight field writing `measurements`).

### Import (OFF)

Per [openfoodfacts-import-hybrid](openfoodfacts-import-hybrid.md): search/barcode → preview → save as own `foods` → use in diary.

### Recipes

Create `foods` with `is_recipe=true` + `recipe_ingredients`; derived macros per 100 g from components (document formula in core tests). Use like any Food in entries.

### Charts

Daily sums, weekly/monthly averages, macro distribution, calorie trend, weight trend from `measurements`.

### Water + copy

Log water toward `water_target_ml`. Copy one meal or entire day to another date.

## Client delivery

- **Core-first** then Android + web in parallel ([platform-delivery](../conventions/agent-dev-environment.md)).
- Web: thin TS + `supabase-js` (ADR 0006); mirror macro math from core.
- Nutrition nav shell (Navigation Compose / react-router) is **intra-module** only.

## Slice order (implementation issues)

1. Docs / ADR 0011 (this spec)  
2. Settings + meal types  
3. Foods CRUD + servings  
4. Nav shell + weekly diary + goal rings  
5. OFF search/import + ranking  
6. Barcode  
7. Recipes  
8. Charts  
9. Water + copy meal/day  

## Acceptance (module-level)

- [ ] User can set goals/height/water target and edit meal types
- [ ] User can CRUD foods with serving units and log diary entries by g/serving
- [ ] Week view with meal sections and goal progress
- [ ] OFF search/import + barcode copy into own foods; attribution shown
- [ ] Recipes usable as foods; charts for macros/calories/weight; water + copy day/meal
- [ ] Android + web parity for the above; core unit tests for macro/serving/OFF mapping

## Related

- [ADR 0011](../decisions/0011-food-data-hybrid-in-phase-3.md), [ADR 0009](../decisions/0009-food-data-source-own-first.md)
- [roadmap](../roadmap.md) phase 3, [OQ-10](../open-questions.md)
