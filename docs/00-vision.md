# Product vision

## What this is

**GymManager** is a personal training super-app: one account, one place for everything training-related. The user tracks nutrition, workouts, and body measurements; the data is theirs and syncs across devices.

The product is intentionally not feature-heavy. The ambition is **professional build, maintenance, and evolution** — a shared core, clear boundaries, documented decisions, and predictable growth.

## Platforms

| Platform | Form | Notes |
|----------|------|--------|
| Android | Native app | Jetpack Compose |
| iOS | Native app | SwiftUI; requires Apple Developer Program |
| Web | Web app | React + TypeScript; used on PC/laptop |

Switching between areas (nutrition / training / measurements) is a UI/UX detail — out of scope for the first documentation iteration.

## Product modules (outline)

1. **Nutrition** — own meals/foods, intake entries, history (1+ year), macros/calories. Inspired by FitAtu / food trackers. MVP catalog source: own foods ([ADR 0009](decisions/0009-food-data-source-own-first.md)).
2. **Training** — exercises, sessions, sets, workout history.
3. **Measurements** — weight and other body metrics over time, charts/aggregations.

Ultimately everything training-related lives in **one** app. Module navigation UX details come later.

## Goals

- Safe, easy-to-maintain sign-in — the user keeps their app data after changing devices.
- Minimum collected user information (PII); the account mainly enables sync and data isolation.
- Shared domain logic in one place (Kotlin Multiplatform), native UI per platform.
- Easy-to-maintain backend: Supabase (Postgres + Auth + RLS).
- Documentation and ADRs as the foundation before code.

## Non-goals (for now)

- AI layer (skills, agents, workflows) — **deliberately deferred** until after infrastructure.
- Community, feed, social features.
- Coaching / plans for other users / multi-tenant B2B.
- Supplement marketplace / e-commerce.
- Full multi-language localization (may return later).
- Final UI/UX polish and branding — foundations and scaffolding first.

## Target user (outline)

Someone who trains and wants nutrition, workout, and measurement history in one place, on phone and computer, without permanently locking data into a closed ecosystem.
