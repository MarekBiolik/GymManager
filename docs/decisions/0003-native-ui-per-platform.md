# ADR 0003 — Natywne UI per platforma

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja planowania architektury)

## Kontekst

Cel platformowy: Android + iOS + web. Właściciel produktu preferuje model „wspólny core, a każda platforma implementuje to, co musi” — maksymalnie natywne UI (SwiftUI, Jetpack Compose, React na webie), zamiast jednego cross-platform frameworka UI.

Świadomy trade-off: **utrzymanie trzech UI** przy niezbyt skomplikowanej apce jest droższe niż Expo/Flutter, ale spójne z wybraną filozofią i jakością natywną.

## Decyzja

Każda platforma ma **własną warstwę UI**:

- Android → Jetpack Compose (Kotlin)
- iOS → SwiftUI (Swift)
- Web → React + TypeScript

Logika biznesowa nie jest reimplementowana w UI — trafia do core ([ADR 0001](0001-shared-core-kmp.md)).

## Alternatywy rozważone

1. **Jeden UI cross-platform (Expo RN / Flutter)** — mniej pracy UI, szybszy feature parity. Odrzucone świadomie na rzecz natywności i modelu core+native.
2. **Tylko web + PWA** — najtaniej, bez App Store. Odrzucone jako jedyna forma: chcemy natywnych aplikacji mobilnych.
3. **Natywne UI per platforma** — wybrane mimo potrójnego kosztu UI.

## Konsekwencje

- Pozytywne: pełny dostęp do platformowych API i look-and-feel; jasny podział odpowiedzialności z core.
- Negatywne: każdy ekran/feature UI ×3; dłuższy time-to-feature; trzeba silnej dyscypliny „logika tylko w core”.
- Później: design system per platforma lub wspólne wytyczne UX; OQ-08 (nawigacja modułów); scaffolding `apps/*`.

## Powiązane

- [clients](../architecture/clients.md)
- [ADR 0001](0001-shared-core-kmp.md)
- [vision](../00-vision.md)
