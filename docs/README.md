# GymManager — dokumentacja

Indeks dokumentacji projektu. Dokumenty są źródłem prawdy dla architektury, domeny i konwencji; kod jest wtórny względem decyzji zapisanych tutaj.

## Start tutaj

1. [Wizja produktu](00-vision.md) — co budujemy, zakres, non-goals
2. [Roadmapa](roadmap.md) — fazy pracy i kiedy precyzujemy poszczególne obszary
3. [Otwarte pytania](open-questions.md) — obszary do doprecyzowania w kolejnych iteracjach

## Architektura

- [Przegląd](architecture/overview.md) — high-level, diagram, struktura repo
- [Stack technologiczny](architecture/tech-stack.md)
- [Wspólny core (KMP)](architecture/shared-core.md)
- [Klienci](architecture/clients.md) — iOS / Android / web
- [Dane i sync](architecture/data-and-sync.md)
- [Autentykacja](architecture/auth.md)

## Decyzje (ADR)

- [Szablon ADR](decisions/adr-template.md)
- [ADR 0001 — Wspólny core: Kotlin Multiplatform](decisions/0001-shared-core-kmp.md)
- [ADR 0002 — Backend: Supabase](decisions/0002-backend-supabase.md)
- [ADR 0003 — Natywne UI per platforma](decisions/0003-native-ui-per-platform.md)

## Domena

- [Słownik pojęć](domain/glossary.md)
- [Model danych (zarys)](domain/data-model.md)

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
