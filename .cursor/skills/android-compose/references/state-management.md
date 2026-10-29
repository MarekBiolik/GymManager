# Android state management (trimmed)

GymManager-specific — not a full Android catalog.

## Prefer

- **Unidirectional data:** screen state from core / ViewModel facades; events up, state down.
- **Hoist state** to the lowest common parent that owns the decision; keep leaf composables mostly stateless.
- **Map at the edge:** core/DTO → UI model in one place; do not re-derive domain rules in Compose.

## Avoid

- Repository or SQL calls from composables that bypass the agreed core facade
- Parallel “domain” ViewModels that reimplement macros, LWW, or auth policy
- Giant god-ViewModels that own unrelated modules — split by screen/flow

Sync/auth complexity stays in `core-kmp`; UI observes results.
