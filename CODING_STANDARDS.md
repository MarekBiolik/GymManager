# Coding standards

Judgement calls for `/code-review`. Prefer a linter or CI check when a rule can be made mechanical.

## Scroll containers

A **scroll container** is the only element that owns `overflow-y: auto` (or `scroll`) for a given column of UI.

- Children of a column flex scroll container that hold full content (summary cards, meal sections, lists) stay **`flex-shrink: 0`**.
- Do not put `overflow: hidden` on those content children to “clip” radius or decoration if it can hide entries, text, or controls when the parent flexes.
- Prefer `border-radius` on the section itself; if clipping is required for radius, ensure the section cannot shrink below its content height (same `flex-shrink: 0` rule).

Violation shape from this repo: meal cards in a flex `day-scroll` shrank and `overflow: hidden` cut off diary entries.
