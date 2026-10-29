# Spec: OpenFoodFacts import/search (nutrition hybrid, post-MVP)

**Status:** SPEC ONLY — **no** OFF HTTP client, **no** barcode SDK, **no** schema apply in this ticket.  
**Issue:** [#26](https://github.com/MarekBiolik/GymManager/issues/26)  
**Parent decision:** [ADR 0009](../decisions/0009-food-data-source-own-first.md) (closes [OQ-07](../open-questions.md)) — phase-3 MVP = own `foods` only  
**Domain:** [data-model — foods](../domain/data-model.md#foods), [glossary](../domain/glossary.md)

This document specifies the **later hybrid**: search (and optionally barcode) against OpenFoodFacts → **copy** into the user’s `foods` with provenance. After import, GymManager never treats OFF as live SoT for the diary.

## Goals

1. Describe the product flow: search / barcode → preview → save as own `foods`.
2. Map OFF fields → canonical macros **per 100 g** + `default_serving_g` when available.
3. Fix post-import rules: local edit allowed; diary stays on `food_entries` snapshots.
4. Record ToS / attribution / rate-limit notes; **forbid** mirroring the entire OFF DB into Postgres.
5. Link from ADR 0009 / phase-3 roadmap as **beyond MVP**.

## Non-goals

| Out of scope | Where |
|--------------|--------|
| OFF client implementation / API keys in code | later implementation ticket after phase-3 own-foods MVP |
| Live re-fetch from OFF when opening a Food | deliberately rejected — local copy is SoT after import |
| Global / shared product catalog in Postgres | ADR 0009 |
| USDA or other sources | future; same “copy into `foods`” pattern if added |
| Changing phase-3 MVP scope (own foods only) | ADR 0009 |

## When this lands

| Prerequisite | Why |
|--------------|-----|
| Phase-3 own-foods CRUD + diary + aggregations working | Hybrid is UX sugar on a working catalog |
| ADR 0009 accepted | Provenance columns and “copy, don’t link” already decided |
| Network error UX patterns from phase 2 available | Search is online-only; write of imported Food follows online-first ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |

Do **not** start this before own-foods MVP is usable. Solo pain of manual entry is the product trigger — same spirit as offline-write (#20): ship when friction is real.

## Flow

```mermaid
sequenceDiagram
  participant U as User
  participant App as Client UI
  participant OFF as OpenFoodFacts API
  participant SB as Supabase Postgres RLS
  U->>App: Search query or barcode
  App->>OFF: Search / product lookup (online)
  OFF-->>App: Candidate list / product payload
  U->>App: Pick candidate → preview macros
  U->>App: Confirm “Save as my food”
  App->>SB: INSERT foods (copy + external_*)
  SB-->>App: own foods row
  Note over App,SB: Later diary entries snapshot macros from foods / entry form — not from OFF
```

### Steps (semantics)

1. **Entry:** nutrition UI → “Search products” / “Scan barcode” (beyond MVP chrome).
2. **Network required** for OFF calls. Offline: show message; do not queue OFF searches. User can still use existing own `foods` from read cache.
3. **Preview:** show mapped name, macros per 100 g, proposed `default_serving_g`, and attribution line (see ToS).
4. **Save:** `INSERT` into user `foods` under RLS — **copy**, not a foreign key to OFF.
5. **After save:** Food behaves like any manually created Food (edit, delete, use in entries).

### Barcode

Optional path using the same import pipeline: barcode → OFF product lookup → same preview → same `INSERT foods`. No requirement to ship barcode in the first hybrid slice; search-only is enough for v1 of this feature.

## Field mapping (OFF → canonical `foods`)

Canonical GymManager shape ([ADR 0009](../decisions/0009-food-data-source-own-first.md)):

| `foods` column | Source / rule |
|----------------|---------------|
| `name` | OFF product name (prefer locale-appropriate name when present; else generic `product_name`) — user may edit after import |
| Macros **per 100 g** | From OFF `nutriments` (or equivalent) values **per 100 g**; map protein / carbs / fat into our columns |
| Derived calories | Prefer compute from P+C+F in domain layer; if OFF energy is present, use as cross-check only — do not invent a second SoT |
| `default_serving_g` | When OFF provides a serving quantity in grams (or convertible), set it; else `null` and UI falls back to amount entry |
| `external_source` | Constant `openfoodfacts` |
| `external_id` | OFF product code / barcode id (string); unique **per user** optional later — not a global unique constraint across users |
| `user_id` | Current Auth user |
| `created_at` / `updated_at` | Server / trigger as usual |

### Mapping edge cases

| Case | Behavior |
|------|----------|
| Missing macros | Block save or allow save with incomplete macros + UI warning — prefer **block** until P/C/F per 100 g exist (data quality) |
| Serving only in non-gram units | Leave `default_serving_g` null; do not guess density |
| Duplicate `external_id` for same user | Offer “open existing Food” instead of second insert (soft dedupe); exact UX in implementation |
| OFF returns multiple locales | Pick one display name; keep `external_id` stable |

## Post-import rules

1. **Local edit:** user may change name, macros, serving — that updates the own `foods` row only; no write-back to OFF.
2. **Diary:** `food_entries` keep a **macro snapshot** at entry time ([data-model](../domain/data-model.md)). Editing the Food later does not rewrite history; re-import from OFF creates a **new** Food or updates only if the user explicitly chooses overwrite of their row.
3. **No live OFF SoT:** opening a Food or diary day never requires OFF. Provenance (`external_*`) is metadata for “where it came from”, not a fetch key for macros at runtime.
4. **Sync:** imported `foods` sync like any other user row (online write + read cache per [ADR 0008](../decisions/0008-sync-strategy-online-first.md)).

## ToS / attribution / rate limits

Operators and implementers must re-read current OFF terms before shipping; summary intent:

| Topic | Rule for GymManager |
|-------|---------------------|
| **Do not mirror OFF** | Forbidden: bulk dump of OFF into our Postgres as a global catalog or “cache the world”. Allowed: on-demand search results briefly in memory + **per-user copies** the user explicitly saved. |
| **Attribution** | Show OFF credit on search results / preview (and settings “data sources” if needed) per current OFF attribution guidelines. |
| **Rate limits** | Client-side debounce; no aggressive prefetch of entire categories; backoff on HTTP 429; prefer official API endpoints documented by OFF. |
| **Personal use copies** | Saving into the user’s private `foods` (RLS) is the product model — not republishing OFF as our dataset. |
| **Licensing changes** | Before implementation, confirm current Open Food Facts data license / API terms and update this section if they diverge. |

Placeholders only in docs/PRs — no API tokens committed. If OFF ever requires a key, it lives in local `.env*` / GitHub Secrets like other backend secrets ([ADR 0004](../decisions/0004-supabase-hosting.md)).

## Network / error UX

| Situation | UX |
|-----------|-----|
| Search while offline | Message: network required for product search; own foods still available |
| OFF timeout / 5xx | Error + retry; do not insert partial Food |
| Empty results | Empty state; CTA to create Food manually |
| Save Food while offline | Same as any write: reject per ADR 0008 (no silent queue in MVP sync) |

## Schema note

Nullable `external_source` / `external_id` may already exist from the nutrition migration (ADR 0009 preference). If not, add them in the hybrid implementation migration — still **no** global OFF mirror table.

## Acceptance criteria (#26)

- [x] Flow: search/barcode → preview → save as own `foods`
- [x] OFF → macros per 100 g + `default_serving_g` mapping rules
- [x] After import: local edit; diary on `food_entries` snapshot
- [x] ToS / attribution / rate-limit; no full OFF DB in Postgres
- [x] Linked from ADR 0009 / phase-3 roadmap as beyond MVP

## Related

- [ADR 0009](../decisions/0009-food-data-source-own-first.md) — own foods first; hybrid deferred here
- [roadmap](../roadmap.md) phase 3
- [ADR 0008](../decisions/0008-sync-strategy-online-first.md) — online search/write
- [#8](https://github.com/MarekBiolik/GymManager/issues/8) — OQ-07 / ADR 0009
