# Spec: account deletion (CASCADE + Edge Function)

**Status:** SPEC ONLY — **no** app code, **no** Edge Function deploy, **no** SQL migration changes in this issue.  
**Issue:** [#13](https://github.com/MarekBiolik/GymManager/issues/13)  
**Behavioral outline:** [auth — Account deletion](../architecture/auth.md#account-deletion)  
**PII / Auth decision:** [ADR 0007](../decisions/0007-auth-providers-minimum-pii.md)  
**FK / RLS schema:** [first migration](first-migration-profiles-rls.md) ([#6](https://github.com/MarekBiolik/GymManager/issues/6); apply = [#21](https://github.com/MarekBiolik/GymManager/issues/21))

This document is the **source of truth** for the account-deletion mechanism in phase 2. Implementation (UI + Edge Function + smoke) is a separate issue after the migration applies.

## Goals

1. Choose the mechanism: **FK `ON DELETE CASCADE` + trusted Edge Function (`service_role`)**, with rationale (not “either–or”).
2. Fix the order: app data disappears with Auth deletion; sessions invalidated.
3. Record store requirements (**in-app** path).
4. Describe RLS implications relative to the first migration (#6).

## Non-goals

| Out of scope | Where |
|--------------|--------|
| Edge Function / client code | phase 2 (implementation) |
| Changing draft SQL #6 | [#6](https://github.com/MarekBiolik/GymManager/issues/6) / [#21](https://github.com/MarekBiolik/GymManager/issues/21) — CASCADE already in the spec |
| Soft-delete / tombstone | beyond MVP ([ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |
| Account deletion “only by emailing support” | conflicts with store requirements |
| Deleting from the client via `anon` Admin API | **forbidden** ([auth — Roles and keys](../architecture/auth.md#roles-and-keys)) |

## Mechanism decision

| Layer | Role | Decision |
|-------|------|----------|
| Schema | Clean up app rows | FK `ON DELETE CASCADE` from user tables / `profiles` → `auth.users(id)` — fixed in [#6](https://github.com/MarekBiolik/GymManager/issues/6) |
| Orchestration | Who may delete Auth identity | **Edge Function** (or equivalent trusted backend) with **`service_role`** + Admin API `deleteUser` |
| Client | UX | Settings → confirm → call Function with user JWT; then local logout / cache clear |

**CASCADE and Edge Function are not alternatives** — they complement each other:

1. **CASCADE alone** is not enough as a product: a client with the `anon` key **cannot** delete an `auth.users` row. Without a trusted path, Auth identity remains (email in IdP/Auth = PII).
2. **Edge Function alone deleting `public.*` rows under `service_role` without CASCADE** is fragile: every new table needs an updated DELETE list; orphaned rows are easy. CASCADE is a schema invariant.
3. **Deleting only via client RLS `DELETE`** removes app data but **not** `auth.users` — the account returns on next OAuth; fails minimum PII / store.

**Chosen:** Function verifies JWT → Admin `deleteUser(uid)` → Postgres removes dependent rows via CASCADE → Auth sessions die with the user.

### Rejected alternatives

| Option | Why not |
|--------|---------|
| CASCADE only (no Function) | No safe trigger to delete Auth from the app |
| Function only + manual DELETE of all tables | Drifts from schema; duplicates CASCADE |
| Database webhook / trigger on `profiles` → delete Auth | Inverted dependency; Auth Admin still needs `service_role`; harder to audit |
| Soft-delete + anonymization | Beyond MVP; stores still expect real account deletion |

## Deletion order

Semantics required by the issue: **app data → Auth; sessions invalidated**.

With CASCADE we achieve this with **one Admin call** (atomic from a product view):

```mermaid
sequenceDiagram
  participant U as User
  participant App as Client
  participant EF as Edge Function
  participant Auth as Supabase Auth Admin
  participant PG as Postgres CASCADE
  U->>App: Delete account + confirm
  App->>EF: POST + JWT (Authorization)
  EF->>EF: verify JWT; uid = claim sub
  EF->>Auth: deleteUser(uid) service_role
  Auth->>PG: DELETE auth.users
  PG-->>Auth: CASCADE profiles + user_id rows
  Auth-->>EF: OK (sessions invalidated)
  EF-->>App: 200
  App->>App: clear session + local cache
```

| Step | What | Notes |
|------|------|--------|
| 1 | Confirmation UX | Deliberate; hard to click by accident (see below) |
| 2 | Edge Function | Only `uid` from a **verified** JWT; **forbid** deleting another `uid` from the body without check |
| 3 | `auth.admin.deleteUser(uid)` | Removes identity; token refresh / sessions stop working |
| 4 | CASCADE | `profiles`, `measurements`, future tables with FK → `auth.users` |
| 5 | Client | Clears local JWT/cache; signed-out UI state |

We do **not** require a separate bulk client `DELETE` before step 3 — CASCADE guarantees no orphans with correct FKs. Optional pre-delete under RLS is unnecessary in MVP and complicates rollback if the network fails before Auth delete.

If Admin `deleteUser` **fails:** app data remains; the user can retry. If `deleteUser` succeeds but the client does not clear cache: local data is stale — on next start the JWT is invalid → forced logout (online-first, [ADR 0008](../decisions/0008-sync-strategy-online-first.md)).

## Confirmation UX

Behavior level (no layout):

1. Entry only from **account settings** (not a one-tap deep link).
2. Explicit copy: deletion is **permanent**; sync and history disappear; signing in again = a **new** empty account.
3. Confirmation: second step (e.g. type a phrase / separate “Delete forever” dialog) — not a single toggle.
4. After success: immediately signed out on this device; other devices lose the session on next token refresh.

## Store requirements (in-app deletion)

| Store | Requirement (direction) | How we meet it |
|-------|-------------------------|----------------|
| **Apple App Store** | Account created in-app must be deletable **in-app** (not only “email support”); process easy to find | Settings → Delete account → confirm → Function |
| **Google Play** | Account-deletion path in the product / visible in-app when we offer accounts | Same path on Android |

Additionally (product, not a separate issue):

- A privacy / data-deletion policy link may exist in the store listing — it does **not** replace the in-app flow.
- Sign in with Apple / Google: we delete the **GymManager** account (Supabase Auth + data); IdP unlinking is the user’s responsibility in Google/Apple settings (out of app scope).

## Relation to #6 and RLS

### First migration ([spec](first-migration-profiles-rls.md))

| #6 element | Role in account deletion |
|------------|--------------------------|
| `profiles.id` → `auth.users` **ON DELETE CASCADE** | Profile gone with Auth delete |
| `measurements.user_id` → `auth.users` **ON DELETE CASCADE** | Smoke + pattern for phases 3–5 |
| Trigger `handle_new_user` | Creates profile; **not** involved in delete |
| Hard delete (no soft-delete) | Consistent with full account removal |

**Rule for future tables:** every user row has FK to `auth.users(id)` (or to `profiles.id` with CASCADE) with **`ON DELETE CASCADE`**, migration together with RLS — otherwise the Function would need a table list.

### RLS implications

| Fact | Consequence |
|------|-------------|
| Own-row `DELETE` policies (#6) | User **may** delete their rows; that is **not** the account-deletion path |
| RLS does **not** protect `auth.users` from Admin API | Therefore the Function uses `service_role` **only** after JWT verification |
| `service_role` bypasses RLS | Function should **not** bulk-scan other users’ tables “just in case”; `deleteUser` + CASCADE is enough |
| `anon` key in the client | May call the Function with a JWT; **never** embed `service_role` |

Function contract sketch (pseudocode — **not** to commit as code in this PR):

```text
POST /functions/v1/delete-account
Authorization: Bearer <user_access_token>

1. Validate JWT (Supabase); reject if missing/invalid
2. uid := jwt.sub
3. admin.deleteUser(uid)   // service_role; never trust body.uid alone
4. return 200
// App rows: CASCADE from auth.users — no manual DELETE list
```

Optionally later: rate limit / audit log (no PII in logs beyond `uid`).

## Implementation checklist (phase 2 — outside this PR)

- [ ] Edge Function `delete-account` (or equivalent name) + `service_role` secret only in Supabase secrets
- [ ] Settings UI on iOS / Android / web — same semantics
- [ ] Smoke: test user → delete → no `profiles`/`measurements` rows; next OAuth = new `auth.users` + empty profile
- [ ] Secrets / CI docs with no `service_role` leak ([ADR 0004](../decisions/0004-supabase-hosting.md))

## Related

- [auth.md#account-deletion](../architecture/auth.md#account-deletion)
- [ADR 0007](../decisions/0007-auth-providers-minimum-pii.md)
- [first-migration-profiles-rls](first-migration-profiles-rls.md) (#6)
- [postgres-invariants](../architecture/postgres-invariants.md)
- [data-and-sync — RLS](../architecture/data-and-sync.md#row-level-security-rls)
- Roadmap phase 2
