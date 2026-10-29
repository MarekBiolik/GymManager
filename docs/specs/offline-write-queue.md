# Spec: offline-write queue and conflict revisit (post-MVP)

**Status:** SPEC ONLY — **no implementation**. Deliberately **beyond MVP**, **not phase 1**, **not phase 2** (“hello sync”).  
**Issue:** [#20](https://github.com/MarekBiolik/GymManager/issues/20)  
**Sync baseline (MVP):** [ADR 0008](../decisions/0008-sync-strategy-online-first.md) — online-first + read cache; online writes only.  
**Implementation:** only after the product trigger below; then a **separate ADR** (or ADR 0008 revisit) + code. This document is a decision map, not permission to code in phases 1–2.

## Goals

1. Close the gap “what we do when offline writes become a requirement” — without building a queue now.
2. Fix the **product trigger** for starting the topic.
3. Outline the **queue model** (local store, ACK, retry, idempotency).
4. Set direction for **tombstone / soft-delete** vs hard delete.
5. Document a **mandatory conflict-rule revisit** when offline-write lands (LWW vs per entity type).
6. Note impact on **web** and the revisit condition in [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md).

## Non-goals

| Out of scope | Where / when |
|--------------|----------------|
| Queue / optimistic UI / tombstone implementation in code | after trigger + new ADR |
| Changing the MVP baseline (online writes only) | [ADR 0008](../decisions/0008-sync-strategy-online-first.md) — stays |
| Read-cache / hello-sync contract (phase 2) | [#19](https://github.com/MarekBiolik/GymManager/issues/19) / [hello-sync-read-cache](hello-sync-read-cache.md) |
| Monorepo scaffold / CI / first migration | phase 1: [#14](https://github.com/MarekBiolik/GymManager/issues/14), [#6](https://github.com/MarekBiolik/GymManager/issues/6), [#21](https://github.com/MarekBiolik/GymManager/issues/21) |
| Mandatory Realtime as sync bus | still rejected as a requirement ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |
| Shared KMP→JS/Wasm engine “so web gets a queue” | only when ADR 0006 revisit conditions are met |

## Product trigger (when we start)

The topic does **not** start “along the way” in phases 1–2 or with the first domain module. We start when **at least one** condition is true:

1. **Offline-write pain** — the user (even solo) regularly loses a workout / food entry / measurement due to no network in the gym or transit, and that blocks daily MVP use.
2. **Product goal “airplane mode”** — a deliberate roadmap decision (e.g. after phase-4 training) that offline write is a release requirement, not a nice-to-have.
3. **Local data as commit** — UI requires optimistic / local commit before ACK (an offline banner is not enough).

Until the trigger: MVP behavior from ADR 0008 (reject offline writes with a message) remains in force.

**Order vs phases:** earliest sensible start is **after** stable hello sync (#19) and after a real training module (phase 4), or earlier only under hard pain (1). **Never** in phase-1 scaffold.

## Queue model (outline — finalize in an implementation ADR)

Postgres remains the **only SoT** ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)). The local queue is a **mutation buffer**, not a second database of truth.

### What we store locally (per queue item)

| Field (direction) | Role |
|-------------------|------|
| `client_mutation_id` (UUID) | Idempotency — same ID on retry; server / client dedupe |
| `entity_table` + `entity_id` | Mutation target (e.g. `sessions`, `sets`, `food_entries`) |
| `op` | `insert` \| `update` \| `delete` |
| `payload` | Row snapshot / delta sufficient to replay |
| `base_updated_at` | `updated_at` seen at edit time (conflict detection on sync) |
| `enqueued_at` / `attempts` / `last_error` | Retry and diagnostics |
| `status` | `pending` \| `in_flight` \| `acked` \| `failed_terminal` |

Read cache (#19) and the write queue are **separate** concerns: the queue does not replace SoT; after ACK we update the cache as for an online write.

### ACK

1. Client sends the mutation with `client_mutation_id` when network returns (FIFO per user / per entity — decide in the implementation ADR; default **global FIFO** on the device, with room to narrow later).
2. API success (2xx + RLS) → status `acked`, remove or archive the item, refresh cache.
3. LWW conflict / version reject → conflict-revisit path (below), not a silent “drop mutation” without policy.

### Retry

- Retry with backoff on network / 5xx errors; **same** `client_mutation_id`.
- Substantive 4xx (RLS, validation) → `failed_terminal` + UI (no infinite retry).
- Attempt limits and “needs user intervention” — in the implementation ADR.

### Idempotency

- Client: stable `client_mutation_id` per mutation.
- Server (direction): uniqueness `(user_id, client_mutation_id)` or equivalent mutation log — **not** in the first MVP migration (#6); only a migration accompanying offline-write.

```text
enqueue(mutation)
  persist local queue row (pending)
  if online: flush()
  else: UI shows pending / offline

flush()
  for each pending in FIFO:
    POST/PATCH/DELETE with client_mutation_id
    on 2xx → acked + update read cache
    on conflict → apply conflict policy (revisit)
    on retryable → backoff, keep pending
```

## Tombstone / soft-delete vs hard delete

| Phase | Deletion |
|-------|----------|
| MVP (current) | **Hard delete** in Postgres + cache invalidation ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |
| With offline-write | **Tombstone / soft-delete** (or equivalent sync tombstone) — required so a queued delete and a delete from another device can reconcile without “resurrecting” the row |

Direction when offline-write lands:

1. A column like `deleted_at timestamptz NULL` (or `is_deleted` + `deleted_at`) on user tables covered by the queue.
2. Sync treats a tombstone as a mutation like update (LWW on the row / rule from the revisit).
3. Hard GC of tombstones — later, with retention; does not block the first offline-write ADR.
4. The first MVP migration (#6) does **not** add tombstones ahead of time — avoid “just in case” schema.

## Conflicts — mandatory revisit at offline-write

MVP deliberately keeps **one** rule: LWW per row by `updated_at` for all entity types ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)).

**When offline-write implementation starts, a conflict revisit is mandatory** (separate section in the implementation ADR or a separate ADR) — do not “bolt on a queue” assuming LWW is enough without checking.

### Questions to close in that ADR

1. Do we **keep one LWW rule** on the whole row for nutrition / training / measurements / `profiles`?
2. Do we introduce **per-entity-type rules** (e.g. merge sets in a session, LWW on body weight, append-only on `food_entries`)?
3. Is a **“resolve conflict” UI** needed, or still silent rules + optional banner “local change overwritten”?
4. How does `base_updated_at` in the queue map to reject / rebase / drop mutation?

### Recommended revisit starting point (not a final decision)

| Entity type (direction) | Hypothesis at revisit start | Why |
|-------------------------|-----------------------------|-----|
| `profiles`, scalar measurements | LWW per row | Simple; rare parallel edits |
| Food entries / sets | LWW or append-only (separate row IDs) | Avoid per-field merge without proof of pain |
| Training session aggregates | Candidate for merge / “union sets” if LWW deletes sets from another device | Most likely pain under offline-write |

Default hypothesis: **stay on LWW** until a concrete scenario (especially offline training on two devices) proves a per-entity rule is needed. The revisit **documents the choice** — even if the choice is “still LWW everywhere”.

## Impact on web (ADR 0006)

Web today: `supabase-js` + thin cache; **no** KMP sync engine ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)).

With offline-write:

| Option | Effect |
|--------|--------|
| A. Offline-write **mobile only** (KMP `core/sync`) | Web stays online-first; document the UX gap; simpler |
| B. Same queue semantics on web (separate TS implementation) | Duplication cost; **parity** condition approaches ADR 0006 revisit item 2 |
| C. Shared engine (KMP→JS/Wasm) | Only when ADR 0006 revisit conditions are met (parity + toolchain + duplication pain) |

**Recorded (closed in this spec):** introducing offline-write **must** explicitly choose A/B/C in the implementation ADR. Default preference until toolchain revisit: **A (mobile-first)** or **B only for a minimal mutation subset** if web has the same product pain. Choosing B or “full parity” **triggers evaluation** of ADR 0006 condition 2 — it does not automatically imply Wasm.

## Relation to ADR 0008

ADR 0008 remains **accepted** for MVP. This spec:

- does **not** change SoT, MVP LWW, or “online writes only”;
- **fills** the “Beyond MVP” section with a concrete queue / tombstone / conflict-revisit / web plan;
- requires a **new ADR** (or formal 0008 revisit) **before** queue code.

## Related

- [ADR 0008](../decisions/0008-sync-strategy-online-first.md), [data-and-sync](../architecture/data-and-sync.md)
- [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md) — parity / revisit
- [#19](https://github.com/MarekBiolik/GymManager/issues/19) — read-cache / hello sync (phase 2; separate track)
- [#6](https://github.com/MarekBiolik/GymManager/issues/6) / [#21](https://github.com/MarekBiolik/GymManager/issues/21) — MVP migration without tombstone / mutation table
- [shared-core](../architecture/shared-core.md) — `core/sync/` = cache now; queue only after this spec + ADR
