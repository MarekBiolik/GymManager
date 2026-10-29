# Documentation lifecycle

Docs record the **current** agreed state: vision, architecture, ADRs, specs, runbooks, conventions. They are not a diary of process, discarded approaches, or completed cleanup chores. Git history (and closed PRs/issues) holds how we got here.

## Keep

- Decisions still in force (ADRs) — context = forces that still matter, not a post-mortem of deleted code
- Specs and runbooks for work not yet fully absorbed into code/policy
- Open questions that are still open (or a short `decided` pointer to the ADR)
- Active phase playbooks under `docs/plans/` **until** the phase results land

## Do not keep

- Narratives about old prototypes, deleted folders, or “we used to…”
- Checklists whose only purpose was a one-off cleanup already finished
- Executed plans/playbooks after their outcomes exist as code, ADR, spec, or convention — **delete the plan** in the same change set that lands the results (or immediately after)
- Duplicate restatements of git/PR process in every ADR

## Flow

1. Short plan (issue / `docs/plans/…` / chat) if needed  
2. Produce durable artifacts (ADR, spec, code)  
3. Remove or shrink the plan; update indexes (`docs/README.md`, roadmap)  
4. Prefer editing the living doc over adding a “changelog” section in docs
