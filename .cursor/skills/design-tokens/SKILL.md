---
name: design-tokens
description: >-
  Own GymManager design-token SSOT under design/tokens/, the thin generator
  under design/, and CI regenerate-diff-clean checks. Use when editing token
  values, generation pipeline, theme outputs, or the user says design tokens /
  theme tokens / Style Dictionary / regen-clean.
---

# Design tokens

Own the **visual value pipeline**: language-neutral SSOT → generator → native theme artifacts. Do **not** implement Compose / SwiftUI / React screens (→ platform skills) or UX flows / IA (→ `ui-ux`).

Decisions and contracts:

- [ADR 0010](../../../docs/decisions/0010-design-tokens-ssot.md) — SSOT + generator; does **not** override native UI ([ADR 0003](../../../docs/decisions/0003-native-ui-per-platform.md))
- [design-tokens contract](../../../docs/specs/design-tokens.md) — layers, naming, paths, CI gate (**SPEC ONLY** until scaffold ships the generator)
- [Fitness/Gym visual style](../../../docs/specs/visual-style-fitness-gym.md) — first theme values (OLED dark)

Deep topics: [references/token-architecture.md](references/token-architecture.md), [references/naming.md](references/naming.md), [references/platform-outputs.md](references/platform-outputs.md).

## Scope split

| This skill | Not this skill |
|------------|----------------|
| `design/tokens/` values and structure | Screen flows, IA, a11y contracts → `ui-ux` |
| Generator under `design/` + regen scripts | Compose/SwiftUI/React feature UI → platform skills |
| CI regenerate-diff-clean gate | Consuming tokens in apps (wire + use) → platform skills |
| Mapping docs for outputs | Domain / RLS / sync → `core-kmp` / `supabase-db` |

Platforms **consume generated output only**. Change a hex or scale here (SSOT), regenerate, then platforms pick up new values — do not hardcode palette in UI.

## Ownership paths

| Path | Role |
|------|------|
| `design/tokens/` | SSOT (primitive → semantic → component) |
| `design/` (generator package) | Thin Node generator (Style Dictionary optional later — ADR 0010) |
| Generated theme files under `apps/*` | **Owned as outputs of this pipeline**; platform skills may wire Theme APIs but must not invent parallel palettes |
| CI job / script for regen-clean | Documented in contract; implement with scaffold [#14](https://github.com/MarekBiolik/GymManager/issues/14) |

## Workflow

Copy and track:

```
Token slice:
- [ ] 1. Confirm layer and style
- [ ] 2. Edit SSOT
- [ ] 3. Regenerate (when generator exists)
- [ ] 4. Verify regen-clean
- [ ] 5. Hand-off
```

### 1. Confirm layer and style

Primitive vs semantic vs component? Dark OLED default vs light (OQ-09 still open)? Prefer semantic edits for role changes; primitives for ramps/scales.

**Done when:** target files and theme mode are named.

### 2. Edit SSOT

Edit JSON under `design/tokens/` per [naming](references/naming.md) and [visual style](../../../docs/specs/visual-style-fitness-gym.md) (or a future alternate style). English-only committed artifacts; docs = current state.

**Done when:** SSOT reflects the intended look; no hex left only in a random app file.

### 3. Regenerate (when generator exists)

Run the documented `design/` generate script. Until scaffold lands the generator, stop after SSOT + spec updates — do **not** invent a full generator in a tokens-docs issue unless AC requires it.

**Done when:** platform outputs match SSOT, or generator absence is explicit.

### 4. Verify regen-clean

Re-run generate; `git diff` on generated paths must be empty. Fix drift by regenerating or fixing the generator — never hand-edit generated files as SoT.

**Done when:** clean tree on generated outputs (or gate still “docs-only expectation”).

### 5. Hand-off

Note which tokens changed; point platforms at consumption docs. PR/merge only if asked. Tick issue AC via `gh`.

**Done when:** platforms can consume without guessing new token names.

## Guardrails

- No application theme **feature** implementation beyond generator outputs (scaffold owns first wiring)
- UI must not hardcode palette hex — fix via SSOT
- English public artifacts; no Polish in repo docs/skills
- Honour [docs lifecycle](../../../docs/conventions/docs-lifecycle.md) — living contracts, not session changelogs
