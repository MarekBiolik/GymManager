# Web theming from design tokens

Pipeline owner: `design-tokens` skill. Contract: [design-tokens.md](../../../../docs/specs/design-tokens.md).

## Outputs (scaffold targets)

| File | Role |
|------|------|
| `tokens.css` (or under `src/styles/`) | CSS custom properties `--color-bg-canvas`, … |
| `tokens.ts` | Typed map for JS-driven values |

## Consumption

- Import/link CSS vars globally; use `var(--color-action-primary)` in CSS modules / global styles.
- Thin theme hook may expose resolved values from CSS or the TS map for inline styles when necessary.
- Preferred mode: dark OLED semantic map until OQ-09.

## Do not

- Duplicate hex in component styles as a second palette
- Hand-edit generated token files — regenerate from `design/tokens/`
