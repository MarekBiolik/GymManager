# Wizja produktu

## Co to jest

**GymManager** to osobista super-aplikacja treningowa: jedno konto, jedno miejsce na wszystko „około treningowe”. Użytkownik śledzi żywienie, treningi i pomiary ciała; dane są jego własne i synchronizują się między urządzeniami.

Produkt celowo nie jest skomplikowany funkcjonalnie. Ambicja leży po stronie **profesjonalnego tworzenia, utrzymania i rozwoju** — wspólny core, jasne granice, dokumentowane decyzje, przewidywalny rozwój.

## Platformy

| Platforma | Forma | Uwagi |
|-----------|--------|--------|
| Android | Aplikacja natywna | Jetpack Compose |
| iOS | Aplikacja natywna | SwiftUI; wymaga Apple Developer Program |
| Web | Aplikacja webowa | React + TypeScript; używana na PC/laptopie |

Przełączanie między obszarami (żywienie / trening / pomiary) to detal UI/UX — poza zakresem pierwszej iteracji dokumentacyjnej.

## Moduły produktowe (zarys)

1. **Żywienie** — własne posiłki/produkty, wpisy spożycia, historia (rok+), makro/kalorie. Inspiracja: FitAtu / trackery żywieniowe. Źródło katalogu w MVP: własne foods ([ADR 0009](decisions/0009-food-data-source-own-first.md)).
2. **Trening** — ćwiczenia, sesje, serie, historia treningów.
3. **Pomiary** — waga i inne pomiary ciała w czasie, wykresy/agregacje.

Docelowo wszystko treningowe żyje w **jednej** aplikacji. Szczegóły UX nawigacji między modułami — później.

## Cele (goals)

- Bezpieczne, łatwe w utrzymaniu logowanie — użytkownik ma swoje dane apki po zmianie urządzenia.
- Minimum zbieranych informacji o użytkowniku (PII); konto służy głównie do sync i izolacji danych.
- Wspólna logika domenowa w jednym miejscu (Kotlin Multiplatform), natywne UI per platforma.
- Backend łatwy w utrzymaniu: Supabase (Postgres + Auth + RLS).
- Dokumentacja i ADR-y jako fundament przed kodem.

## Non-goals (na teraz)

- Warstwa AI (skille, agenci, workflow) — **świadomie odłożona** na fazę po infrastrukturze.
- Społeczność, feed, social features.
- Coaching / plany dla innych użytkowników / multi-tenant B2B.
- Marketplace suplementów / e-commerce.
- Pełna lokalizacja wielojęzyczna (może wrócić później).
- Finalny polish UI/UX i branding — najpierw fundamenty i scaffolding.

## Użytkownik docelowy (zarys)

Osoba trenująca, która chce trzymać historię żywienia, treningów i pomiarów w jednym miejscu, na telefonie i na komputerze, bez oddawania danych „na zawsze” do zamkniętego ekosystemu.

## Relacja do starego prototypu

Wcześniej istniał lokalny prototyp (web React/Vite + serwer Node + SQLite + OpenFoodFacts). Źródła zniknęły; zostały artefakty. Traktujemy projekt jako **czysty restart „on top”**. Decyzja o losie pozostałości: [legacy-cleanup](conventions/legacy-cleanup.md).

## Status dokumentu

Zatwierdzony jako fundament iteracji 1. Szczegóły modułów, modelu danych i auth — do doprecyzowania w kolejnych planach (patrz [roadmap](roadmap.md) i [open-questions](open-questions.md)).
