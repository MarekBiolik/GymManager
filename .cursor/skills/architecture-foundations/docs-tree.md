# Default docs tree

Adapt names/paths if the user already has a docs layout. Create only what the plan lists.

```
docs/
  README.md                 # index + reading order
  00-vision.md              # product, platforms, goals, non-goals
  roadmap.md                # phases: foundations → scaffold → modules → …
  open-questions.md         # OQ-NN, status, close-by phase
  architecture/
    overview.md             # diagram + layers + repo layout target
    tech-stack.md           # technologies by role (versions later)
    shared-core.md          # optional if shared library/core exists
    clients.md              # per-platform UI + risky interop
    data-and-sync.md        # source of truth, RLS/authz, sync outline
    auth.md                 # identity, PII minimum, keys
  decisions/
    adr-template.md
    0001-….md               # one ADR per accepted foundational choice
    0002-….md
    0003-….md
  domain/
    glossary.md
    data-model.md           # entity outline, not final migrations
  conventions/
    coding-standards.md
    git-workflow.md
    testing.md
    ci-cd.md
    legacy-cleanup.md       # only if restart / debris present
```

## ADR minimum set (examples)

Map to whatever was actually decided:

1. Shared logic strategy (e.g. KMP core / RN / Flutter / backend-only)
2. Backend + data store (e.g. Supabase/Postgres, Firebase, custom)
3. UI strategy (native per platform vs cross-platform UI)

## Typical open questions to seed

- Web ↔ shared-core interop (if multiplatform core)
- Exact auth method vs minimum PII
- Offline / conflict strategy
- Monorepo tooling
- Hosting + backups
- Legacy artifact disposal
- Module UX / navigation (often late phase)
- External data sources (e.g. food DB)

## Roadmap phase shape

0. Foundations (this skill)  
1. Scaffolding monorepo + env  
2. Auth + sync baseline  
3+. Product modules  
Last. AI / agents (after infra), unless user prioritizes otherwise
