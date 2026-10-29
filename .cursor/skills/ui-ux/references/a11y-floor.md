# Accessibility floor

Aligned with [visual-style-fitness-gym](../../../../docs/specs/visual-style-fitness-gym.md).

## Contrast

- Body text on surfaces: **≥ 4.5:1**
- Large text: ≥ 3:1 minimum; prefer ≥ 4.5:1
- If a combination fails, fix **semantic tokens** via `design-tokens` — do not patch one screen with a hardcoded hex

## Color and state

Color is never the sole cue — pair with label, icon, or pattern (success / warn / error).

## Interaction

- Icon-only controls need accessible names
- Focus order / keyboard on web; adequate touch targets on mobile
- Honour reduced-motion when proposing motion tokens / animations

## Platform detail

Compose / web / SwiftUI a11y APIs live in platform skills; this floor is the product expectation those implementations must meet.
