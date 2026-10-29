# Skill `references/` convention

How GymManager Cursor skills keep `SKILL.md` lean while still holding deep topics.

## Rule

- **`SKILL.md`** — triggers (description), scope split, ordered workflow with done criteria, guardrails. Keep it short enough that every run can follow the steps.
- **`references/`** — disclosed deep topics loaded when that branch of the work fires (theming details, naming tables, env checklists, a11y floors).

Do **not** micro-split a new top-level skill for every deep topic. Prefer one skill per ownership surface + `references/` files.

## When to add a reference file

Add `references/<topic>.md` when:

1. Only some runs need the material (branching disclosure), or
2. Tables / mappings would bury the workflow steps in `SKILL.md`, or
3. Platform-specific consumption notes would duplicate across the main skill body.

Keep a short pointer in `SKILL.md` to each reference that agents commonly need.

## When not to

- Do not copy external skill catalogs wholesale.
- Do not put the primary workflow only in `references/` — steps stay in `SKILL.md`.
- Do not change [platform-delivery](../../.cursor/skills/platform-delivery/SKILL.md) order (core-first → parallel platforms → tests) to “make room” for docs.

## Index

Project skill routing (when to load each skill + which `references/` exist): [platform-delivery — Skill routing](../../.cursor/skills/platform-delivery/SKILL.md#skill-routing). Living overview: [agent-dev-environment](agent-dev-environment.md).

## English

All skill and reference text committed to the repo is English (public artifacts rule).
