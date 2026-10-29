# GymManager — dokumentacja

Indeks dokumentacji projektu. Dokumenty są źródłem prawdy dla architektury, domeny i konwencji; kod jest wtórny względem decyzji zapisanych tutaj.

## Start tutaj

1. [Wizja produktu](00-vision.md) — co budujemy, zakres, non-goals
2. [Roadmapa](roadmap.md) — fazy pracy i kiedy precyzujemy poszczególne obszary
3. [Otwarte pytania](open-questions.md) — obszary do doprecyzowania w kolejnych iteracjach

## Plany (playbooki faz)

- [Faza 1 — playbook scaffoldu monorepo](plans/phase-1-scaffold-playbook.md) — kolejność, done criteria, CI, Supabase `dev`; implementacja = issue #14

## Architektura

- [Przegląd](architecture/overview.md) — high-level, diagram, struktura repo
- [Stack technologiczny](architecture/tech-stack.md)
- [Wspólny core (KMP)](architecture/shared-core.md)
- [Klienci](architecture/clients.md) — iOS / Android / web
- [Dane i sync](architecture/data-and-sync.md)
- [Wspólne niezmienniki Postgres](architecture/postgres-invariants.md) — RLS / CHECK vs core/TS (po ADR 0006)
- [Autentykacja](architecture/auth.md)

## Decyzje (ADR)

- [Szablon ADR](decisions/adr-template.md)
- [ADR 0001 — Wspólny core: Kotlin Multiplatform](decisions/0001-shared-core-kmp.md)
- [ADR 0002 — Backend: Supabase](decisions/0002-backend-supabase.md)
- [ADR 0003 — Natywne UI per platforma](decisions/0003-native-ui-per-platform.md)
- [ADR 0004 — Hosting Supabase i backupy](decisions/0004-supabase-hosting.md)
- [ADR 0005 — Monorepo: layout, tooling i wersjonowanie core](decisions/0005-monorepo-tooling.md)
- [ADR 0006 — Web: supabase-js bez KMP→JS/Wasm](decisions/0006-web-supabase-js-without-kmp-js.md)
- [ADR 0007 — Providery logowania i minimum PII](decisions/0007-auth-providers-minimum-pii.md)
- [ADR 0008 — Strategia sync: online-first + read cache](decisions/0008-sync-strategy-online-first.md)

## Domena

- [Słownik pojęć](domain/glossary.md)
- [Model danych (zarys)](domain/data-model.md)

## Specyfikacje (SQL / migracje — draft)

- [Pierwsza migracja: profiles + baseline RLS](specs/first-migration-profiles-rls.md) — SPEC ONLY; apply = [#21](https://github.com/MarekBiolik/GymManager/issues/21); draft SQL w `docs/specs/sql/` (**NOT APPLIED**)
- [Kolejka offline-write i revisit konfliktów](specs/offline-write-queue.md) — SPEC ONLY; **post-MVP** / nie faza 1 ([#20](https://github.com/MarekBiolik/GymManager/issues/20)); bez implementacji teraz

## Konwencje

- [Standardy kodu](conventions/coding-standards.md)
- [Git workflow](conventions/git-workflow.md)
- [Testowanie](conventions/testing.md)
- [CI/CD](conventions/ci-cd.md)

## Legacy / cleanup

- [Los artefaktów prototypu](conventions/legacy-cleanup.md)

## Zasady pracy z dokumentacją

- Nowe decyzje architektoniczne → ADR w `docs/decisions/`.
- Niejasności i odroczone tematy → `open-questions.md`, nie „rozwiązywanie w kodzie po cichu”.
- Doprecyzowanie obszaru = osobna iteracja (plan + aktualizacja docs), potem scaffolding/kod.
