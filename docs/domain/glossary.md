# Domain glossary

Unambiguous names used in docs, ADRs, code, and schema. Add new terms here **before** synonyms diverge across PRs.

## Product

| Term | Meaning |
|------|---------|
| **GymManager** | Project / personal training super-app name |
| **Module** | Product area in one app: nutrition, training, measurements |
| **Account** | Supabase Auth identity; used for sync and data isolation, not a social profile |
| **IdP** | Identity Provider — here Google and Sign in with Apple (iOS); decision: [ADR 0007](../decisions/0007-auth-providers-minimum-pii.md) |
| **PII** | Personally identifiable information; GymManager collects the minimum (email only in Auth from IdP, not in `profiles`) |

## Nutrition

| Term | Meaning |
|------|---------|
| **Food** | Product or recipe defined by the user (or imported), with macros per serving/100 g |
| **Food entry** | Intake event: what, how much, when (linked to a Food or a value snapshot) |
| **Diary** | Day view of entries (UI); not necessarily a separate table |
| **Macro** | Protein, carbs, fat (and derived calories) |

## Training

| Term | Meaning |
|------|---------|
| **Exercise** | Exercise (e.g. squat); template, not an execution |
| **Workout / Session** | A concrete training session in time |
| **Set** | A set within a session (reps, weight, RPE… — fields to refine) |
| **Template** | Optional reusable training plan/template (later) |

## Measurements

| Term | Meaning |
|------|---------|
| **Measurement** | Body metric over time (weight, girths…); type + value + timestamp |
| **Metric type** | Measurement kind (e.g. `body_weight_kg`) |

## Technical (used in the data domain)

| Term | Meaning |
|------|---------|
| **Core** | Shared KMP module with domain logic |
| **RLS** | Row Level Security — row isolation per `user_id` |
| **Sync** | Aligning client state with Postgres (MVP: online write + read cache — [ADR 0008](../decisions/0008-sync-strategy-online-first.md)) |
| **Source of truth** | Postgres; local read cache is secondary |
| **LWW** | Last-write-wins — row conflict resolved by later `updated_at` |
| **Read cache** | Local copy of fetched rows; not an offline write queue |

## Avoid

- Confusing **Food** (definition) with **Food entry** (intake event).
- Synonyms “training / workout / session” without precision — in schema prefer the fixed table names from [data-model](data-model.md).
- “User profile” in a social sense — we have at most a technical `profiles` row.
