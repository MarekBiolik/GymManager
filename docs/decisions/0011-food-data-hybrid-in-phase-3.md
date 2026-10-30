# ADR 0011 — Food data hybrid in phase 3

- **Status:** accepted
- **Date:** 2026-10-30
- **Decision makers:** Marek (phase-3 planning)
- **Supersedes:** [ADR 0009](0009-food-data-source-own-first.md) (MVP = own foods only; hybrid deferred)

## Context

Phase 3 builds a personal Fitatu-like nutrition tracker. ADR 0009 deferred OpenFoodFacts (OFF) search/import until after own-foods MVP to keep scope small. Product intent for phase 3 is now: **own catalog as the fast path**, with **OFF search/import and barcode** in the same phase so users are not forced into pure manual entry. Forces: solo maintenance, diary must not depend on OFF at runtime, no global product mirror in Postgres, kg/g-only units for now.

## Decision

1. **Phase 3 includes hybrid food data:** user-owned `foods` remain the only diary SoT; OFF is search/import (and barcode → lookup) that **copies** into `foods` with `external_source` / `external_id`. After import, local edit and `food_entries` macro snapshots behave as in ADR 0009.
2. **No live OFF SoT** for opening a Food or diary day; no mirroring the entire OFF DB into Postgres.
3. **Starter catalog:** start **empty**. If OFF coverage of Polish basics (veg/fruit/bread) is too weak before the app is “done”, add a small hardcoded per-user seed — tracked as [OQ-10](../open-questions.md).
4. **Imported foods are permanent** own rows. “Recently used” / favorites are **ranking only** (`last_used_at` / flags) — no LRU auto-eviction of catalog rows.
5. **OFF access:** direct client HTTP (Ktor in core for mobile; `fetch` on web). Debounce, attribution, rate-limit backoff. Edge Function proxy only if rate limits force it later.
6. The hybrid flow/rules in [openfoodfacts-import-hybrid](../specs/openfoodfacts-import-hybrid.md) move from “beyond MVP” into **phase-3 scope** (implementation tickets after the nutrition module spec).

## Alternatives considered

1. **Keep ADR 0009 (own foods only in phase 3)** — simplest schema/UI. Rejected: product wants Fitatu-like add-by-name/scan without waiting for a later phase.
2. **Shared curated system catalog in Postgres** — Rejected for start: maintenance cost; does not replace OFF for packaged goods.
3. **LRU eviction of imported foods above N** — Rejected: surprise data loss; ranking is enough for fast access.

## Consequences

### Positive

- Phase 3 ships a usable diary without requiring every product to be typed by hand.
- Mental model stays “own `foods` + entry snapshots”; hybrid does not introduce a second SoT.
- Clear path for barcode as a thin path into the same import pipeline.

### Negative / costs

- Phase 3 scope is larger than ADR 0009 assumed (network UX, OFF mapping, attribution, camera permissions).
- Web and core each talk to OFF (no shared JS OFF client under ADR 0006).
- Empty start may feel sparse until the user imports or until OQ-10 seed lands.

### What ADR 0009 still holds

- Own `foods` under RLS; macros per 100 g + optional servings; entry-time macro snapshot; copy-not-link import; no global OFF SoT.

## Related

- Supersedes phase-3 boundary of [ADR 0009](0009-food-data-source-own-first.md)
- [openfoodfacts-import-hybrid](../specs/openfoodfacts-import-hybrid.md), [nutrition-module](../specs/nutrition-module.md)
- [roadmap](../roadmap.md) phase 3, [OQ-10](../open-questions.md), [data-model](../domain/data-model.md)
