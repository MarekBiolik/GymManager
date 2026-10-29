# Token architecture

Full contract: [docs/specs/design-tokens.md](../../../../docs/specs/design-tokens.md). ADR: [0010](../../../../docs/decisions/0010-design-tokens-ssot.md).

## Layers

1. **Primitive** — raw ramps and scales (`color.orange.500`, `space.4`, `motion.duration.fast`). No UI role.
2. **Semantic** — role aliases (`color.bg.canvas`, `color.action.primary`). **Theme mode (dark/light) lives here.** Preferred default: dark OLED.
3. **Component** — optional aliases for recurring patterns (`button.primary.bg`). Keep thin; prefer semantic when enough.

## Resolution

Semantic/component `$value` may reference `{path.to.primitive}` (or another alias). Generator resolves before emit.

## Style swap

Edit primitives and/or semantic maps → regenerate → apps update. Do not rewrite screens for a rebrand. Alternate styles / light mode: [OQ-09](../../../../docs/open-questions.md).

## First style seed

[Fitness/Gym visual style](../../../../docs/specs/visual-style-fitness-gym.md) — Orange `#FF6B35`, Electric Blue, OLED near-black.
