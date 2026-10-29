# Token naming

Contract detail: [docs/specs/design-tokens.md](../../../../docs/specs/design-tokens.md#naming-convention).

## Pattern

```text
--{category}-{item}-{variant?}-{state?}
```

JSON paths use the same segments: `color.action.primary`, `color.action.primary.pressed`.

| Segment | Examples |
|---------|----------|
| category | `color`, `space`, `radius`, `type`, `elevation`, `motion` |
| item | `bg`, `text`, `action`, `4`, `md`, `fast` |
| variant | `primary`, `canvas`, `muted` |
| state | `hover`, `pressed`, `disabled`, `focus` |

## Platform flattening

| Form | Example |
|------|---------|
| CSS | `--color-action-primary` |
| TypeScript | `colorActionPrimary` or nested `tokens.color.action.primary` |
| Kotlin | `ColorActionPrimary` / `GymTokens.Color.ActionPrimary` |
| Swift | `Color.actionPrimary` / `GymTokens.Color.actionPrimary` |

Keep names stable once apps consume them — renames need a coordinated regenerate + UI grep.
