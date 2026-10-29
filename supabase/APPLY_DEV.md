# Apply migrations to Cloud `dev`

After linking ([README.md](README.md)):

```bash
npx supabase db push
```

Prefer CLI so `supabase/migrations/` stays source of truth. Before risky applies: dump per [#9](https://github.com/MarekBiolik/GymManager/issues/9).

First migration: `migrations/20261029190000_profiles_baseline_rls.sql` ([#21](https://github.com/MarekBiolik/GymManager/issues/21)).
