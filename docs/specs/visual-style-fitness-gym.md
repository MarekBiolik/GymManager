# Spec: GymManager visual style — Fitness/Gym (vibrant, block-based, OLED dark)

**Status:** SPEC ONLY — no generator implementation, no platform theme code in this ticket.  
**Issue:** [#62](https://github.com/MarekBiolik/GymManager/issues/62)  
**Parent decision:** [ADR 0010](../decisions/0010-design-tokens-ssot.md)  
**Token contract:** [design-tokens](design-tokens.md) ([#61](https://github.com/MarekBiolik/GymManager/issues/61))

This document defines the **first** swappable GymManager visual style: Fitness/Gym — vibrant, block-based composition, preferred **dark OLED**. Values seed the SSOT under `design/tokens/`. Inspiration mapping only (catalog “Fitness/Gym App → Vibrant & Block-based + OLED dark”); no external tooling dependency.

**This is one theme via the token SSOT — not a forever hardcoded look.** UI must consume generated semantic tokens; do **not** hardcode hex in Compose / SwiftUI / React.

## Goals

1. Concrete palette and semantic roles for the first theme.
2. Type, space (4px base), radius, elevation, motion token scales.
3. Block / bento layout intent for screens.
4. Accessibility floor (contrast, no color-only cues).
5. Preferred mode dark OLED; light deferred to [OQ-09](../open-questions.md) governance.

## Non-goals

| Out of scope | Where |
|--------------|--------|
| Generator or `apps/*` theme wiring | scaffold [#14](https://github.com/MarekBiolik/GymManager/issues/14), [design-tokens](design-tokens.md) |
| Light-mode palette finalization | OQ-09 |
| Module navigation IA | OQ-08 / phase 6 |
| Full component library Figma export | later polish |

## Preferred mode

- **Default / preferred:** dark OLED (near-black surfaces, vibrant accents).
- **Light mode:** deferred — whether and when to ship is OQ-09; do not block dark-first UI on light tokens.

## Palette

### Brand / accent primitives

| Token path (illustrative) | Hex | Role |
|---------------------------|-----|------|
| `color.orange.500` | `#FF6B35` | Primary energetic brand / primary action |
| `color.blue.500` | `#00D4FF` | Electric Blue secondary accent / highlights |
| `color.blue.600` | `#00A8CC` | Electric Blue pressed / quieter accent |

Adjust blue hex slightly in SSOT if contrast checks require it; keep the **Electric Blue** intent.

### OLED surfaces (dark semantic)

| Semantic token | Suggested value | Role |
|----------------|-----------------|------|
| `color.bg.canvas` | `#000000` | App canvas (true OLED black where possible) |
| `color.bg.surface` | `#0A0A0A` | Raised block / card surface |
| `color.bg.surface-elevated` | `#141414` | Nested block / sheet |
| `color.bg.muted` | `#1A1A1A` | Subtle inset / disabled well |

### Text (dark)

| Semantic token | Suggested value | Role |
|----------------|-----------------|------|
| `color.text.primary` | `#F5F5F5` | Body / titles on canvas |
| `color.text.secondary` | `#A3A3A3` | Supporting copy |
| `color.text.inverse` | `#0A0A0A` | Text on bright accent fills |
| `color.text.brand` | `#FF6B35` | Brand-emphasized labels (sparingly) |

### Actions / feedback

| Semantic token | Suggested value | Role |
|----------------|-----------------|------|
| `color.action.primary` | `{color.orange.500}` → `#FF6B35` | Primary CTA fill |
| `color.action.secondary` | `{color.blue.500}` → `#00D4FF` | Secondary CTA / links |
| `color.border.subtle` | `#2A2A2A` | Block edges when needed |
| `color.feedback.success` | `#22C55E` | Success |
| `color.feedback.warn` | `#F59E0B` | Warning |
| `color.feedback.error` | `#EF4444` | Error |

### Contrast

- Body text on canvas/surface: target **≥ 4.5:1** (WCAG AA for normal text).
- Large title text: ≥ 3:1 minimum; prefer ≥ 4.5:1 when practical.
- Do **not** use color as the sole cue for state (pair with icon, label, or pattern).

## Type scale

Base: readable sans for UI (platform default stacks OK until a brand typeface is chosen in polish). Scale steps (rem / sp / pt equivalents via generator):

| Token | Size | Line height | Use |
|-------|------|-------------|-----|
| `type.display.size` | 32 | 40 | Rare hero numbers |
| `type.title.lg.size` | 24 | 32 | Screen titles |
| `type.title.md.size` | 20 | 28 | Section titles |
| `type.body.lg.size` | 16 | 24 | Primary body |
| `type.body.md.size` | 14 | 20 | Secondary body |
| `type.label.sm.size` | 12 | 16 | Meta / chips |
| `type.weight.regular` | 400 | — | Body |
| `type.weight.semibold` | 600 | — | Titles / emphasis |
| `type.weight.bold` | 700 | — | Display / strong CTA |

## Spacing scale (4px base)

| Token | px |
|-------|----|
| `space.1` | 4 |
| `space.2` | 8 |
| `space.3` | 12 |
| `space.4` | 16 |
| `space.5` | 20 |
| `space.6` | 24 |
| `space.8` | 32 |
| `space.10` | 40 |
| `space.12` | 48 |
| `space.16` | 64 |

Layout rhythm: prefer multiples of 4; section gaps typically `space.6`–`space.10`.

## Radius

| Token | px | Use |
|-------|----|-----|
| `radius.sm` | 8 | Dense controls |
| `radius.md` | 12 | Default blocks / buttons |
| `radius.lg` | 16 | Large bento tiles |
| `radius.full` | 9999 | Avatars / circular only — avoid pill clusters for chrome |

Block-based style favors **md/lg** rounded rectangles over tiny chips.

## Elevation / shadow (OLED)

On true black, heavy shadows are weak; prefer **surface step** over deep drop shadows.

| Token | Intent |
|-------|--------|
| `elevation.0` | Flat on canvas |
| `elevation.1` | Surface step (`bg.surface` vs canvas) — primary “lift” |
| `elevation.2` | Slight soft shadow `0 4px 16px` at ~40% black + elevated surface |
| `elevation.3` | Modal / sheet: stronger dim scrim + `bg.surface-elevated` |

Avoid multi-layer neon glows as the default chrome language.

## Motion

| Token | ms | Use |
|-------|----|-----|
| `motion.duration.fast` | 120 | Pressed / toggle |
| `motion.duration.normal` | 200 | Block appear / color shift |
| `motion.duration.slow` | 320 | Screen-level transitions |
| `motion.easing.standard` | platform standard / ease-out | Default |
| `motion.easing.emphasized` | ease-in-out | Deliberate hero motions |

Ship intentional motion in polished screens (2–3 purposeful moments), not decorative noise.

## Block / bento layout intent

- **One job per section:** one headline, usually one short supporting line, one primary action cluster.
- **Block-based composition:** large rectangular tiles (bento) for primary modules (nutrition, training, measurements) — not a dense dashboard of unrelated cards in the first viewport.
- Prefer full-width or half-width blocks with clear hierarchy over floating sticker chrome.
- First viewport of marketing/landing surfaces (when any): brand-forward, one CTA group — follow product UX rules; app shells use the same block rhythm with module content.
- Cards only when they contain a real interaction or a distinct navigable unit; if removing border/shadow/radius does not hurt understanding, skip the card chrome.

## Swappability

- All values above land in `design/tokens/` as primitives + dark semantics.
- Restyle = token edit + regenerate ([design-tokens](design-tokens.md) CI gate).
- Platforms bind to semantic names (`color.action.primary`), never to `#FF6B35` in feature UI.

## Related

- [design-tokens](design-tokens.md), [ADR 0010](../decisions/0010-design-tokens-ssot.md)
- Theming governance: [open-questions](../open-questions.md) OQ-09
- Phase 6 polish still owns nav coherence / store polish — not this first token set
