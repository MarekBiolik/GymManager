# ADR 0009 — Food data source: own foods first (MVP)

- **Status:** accepted
- **Date:** 2026-10-29
- **Decision makers:** Marek (+ OQ-07 closing session)

## Context

The nutrition module (phase 3) needs a product catalog with macros from which **Food entries** are created. Forces: solo maintenance, control over data quality (especially local / store products), offline read of already-cached foods ([ADR 0008](0008-sync-strategy-online-first.md)), no dependency on an external API on the diary critical path, and a fast MVP without HTTP integration to a third-party DB.

Options from [OQ-07](../open-questions.md): own foods only, OpenFoodFacts (OFF) live-only, or hybrid (external search + local own foods).

## Decision

1. **MVP (phase 3):** **own foods only** — the user creates and edits products in their `foods` table (Postgres + RLS). No OpenFoodFacts (or other external catalog) calls in MVP code. No global product table shared across users.
2. **Macro SoT for entries:** as in [data-model](../domain/data-model.md) — `food_entries` keep a **snapshot** of macros at entry time; editing `foods` does not rewrite history. The diary does not depend on external API availability.
3. **Canonical nutrition values in `foods` (MVP):** macros **per 100 g** + optional `default_serving_g` (default UI portion). Derived calories / P+C+F consistency — rule in core (mobile) / thin web validation; CHECK only for sensible ranges if needed ([postgres-invariants](../architecture/postgres-invariants.md)).
4. **Later (hybrid, beyond nutrition MVP):** optional **search / import** from OpenFoodFacts (or another source) that **copies** name and macros into a **new user `foods` row**. After import the product is a normal own Food — editable, synced like the rest. Provenance columns: `external_source` (e.g. `openfoodfacts`) + `external_id` (nullable); **not** required at runtime to read the diary.
5. **Barcode / live lookup on every meal:** beyond MVP. If added later — still save a local copy in `foods`, not an “OFF link only”.

## Alternatives considered

1. **Own foods only, no hybrid path** — simplest. Rejected as the long-term end state: ready-product search UX is valuable; we keep a **clear** hybrid follow-up instead of closing the door.
2. **OFF-only (live API on every pick)** — rich catalog from day 1. Rejected: PL completeness/quality, OFF rate limits / ToS, no offline for new products, HTTP client + field mapping maintenance, history tied to an external id.
3. **Hybrid from day 1 (OFF search + own)** — nicer UX earlier. Rejected for MVP: doubles phase-3 scope (search UI, field mapping, network errors, license/attribution) before own-foods CRUD + diary + aggregations work.
4. **Shared curated catalog in Postgres** — quality control. Rejected at start: solo content maintenance cost; does not solve “quick add yoghurt from the shop” without manual entry or import anyway.

## Consequences

### Positive

- Phase 3 builds on the already agreed model (`foods` / `food_entries`, RLS, LWW) without a new integration.
- Offline: once-synced `foods` live in the read cache ([ADR 0008](0008-sync-strategy-online-first.md)); no OFF dependency when logging a meal (writes remain online-only under MVP sync).
- Data quality under user control; a bad OFF import can be fixed locally.
- Later hybrid needs no mental-model migration: `external_*` is optional provenance on an own row.

### Negative / costs

- MVP requires manual product entry (UX friction vs trackers with full OFF).
- Duplicate names per user are possible — dedupe / merge later, not in MVP.
- When OFF import arrives it must respect ToS / attribution and must **not** mirror the entire OFF DB in our Postgres as a global SoT.

### MVP vs later (boundary)

| In MVP (phase 3) | Beyond MVP (follow-up) |
|------------------|------------------------|
| CRUD `foods` (user-owned) | OFF search / barcode → import into `foods` |
| `food_entries` + macro snapshot | Live re-fetch from OFF when opening a product |
| Macros per 100 g + `default_serving_g` | Shared system catalog / curated DB |
| Nullable `external_source` / `external_id` in schema (may stay unused) | Other sources (USDA, store DBs) |
| Daily aggregations from entries | Community sharing of products across users |

`external_*` columns **may** be added in the nutrition migration as null (to avoid a second migration) — or only when hybrid lands; preference at phase-3 spec: **add nullable immediately**, with no OFF UI/API.

## Related

- Closes [OQ-07](../open-questions.md)
- [data-model](../domain/data-model.md), [glossary](../domain/glossary.md), [roadmap](../roadmap.md) phase 3
- [ADR 0008](0008-sync-strategy-online-first.md) — sync / read cache for `foods`
- [postgres-invariants](../architecture/postgres-invariants.md) — ownership `food_entries` → `foods`
- Follow-up hybrid spec: [openfoodfacts-import-hybrid](../specs/openfoodfacts-import-hybrid.md) ([#26](https://github.com/MarekBiolik/GymManager/issues/26))
