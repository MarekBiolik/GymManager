# delete-account Edge Function

Trusted account deletion for GymManager ([spec](../../../docs/specs/account-deletion.md)).

## Contract

```http
POST /functions/v1/delete-account
Authorization: Bearer <user_access_token>
```

- Verifies the caller JWT; `uid` comes from the token (`sub`), not from the body.
- Calls Auth Admin `deleteUser(uid)` with `service_role` (injected at deploy — never in git or clients).
- Postgres removes `profiles`, `measurements`, and other user FK rows via `ON DELETE CASCADE`.

## Deploy (Cloud `dev`)

Prerequisites: [linked project](../../README.md), migration [#21](../../migrations/20261029190000_profiles_baseline_rls.sql) applied.

```bash
npx supabase functions deploy delete-account
```

Supabase injects `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` for deployed functions. Do **not** commit the service role key; store it only in the Supabase dashboard / password manager.

JWT verification at the gateway stays enabled (`verify_jwt = true` in [config.toml](../../config.toml)).

## Maintainer smoke (manual)

1. Create a test user in Cloud `dev` (OAuth or email).
2. Insert or confirm `profiles` / optional `measurements` rows exist for that user.
3. Call the function with the user’s access token (e.g. `curl -X POST "$SUPABASE_URL/functions/v1/delete-account" -H "Authorization: Bearer $ACCESS_TOKEN" -H "apikey: $SUPABASE_ANON_KEY"`).
4. Expect `200` and `{ "ok": true }`; user gone from Auth; no app rows for that `user_id`.
5. Sign in again with the same IdP → new `auth.users` row and empty profile.
