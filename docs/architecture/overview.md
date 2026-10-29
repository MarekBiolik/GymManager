# Architektura — przegląd

## Idea w jednym zdaniu

Wspólny **core domenowy** (Kotlin Multiplatform) + **natywne UI** na iOS, Android i web + **Supabase** jako backend (Postgres, Auth, RLS, Realtime, Storage).

## Diagram

```mermaid
flowchart TB
  subgraph clients [Klienci - natywne UI]
    ios["iOS: SwiftUI"]
    android["Android: Jetpack Compose"]
    web["Web: React + TypeScript"]
  end
  core["Wspolny core: Kotlin Multiplatform\n(domena, repozytoria, sync, networking)"]
  supa["Supabase\nPostgres + Auth + RLS + Realtime + Storage"]
  ios --> core
  android --> core
  web -->|"supabase-js (ADR 0006)"| supa
  core --> supa
```

## Warstwy

| Warstwa | Odpowiedzialność | Nie robi |
|---------|------------------|----------|
| UI (per platforma) | Prezentacja, nawigacja, input, platformowe API (kamera, powiadomienia) | Reguł biznesowych, SQL, polityki auth |
| Core (KMP) | Model domenowy, use-case'y, repozytoria, sync, walidacje | Widgetów UI, layoutu |
| Supabase | Persistencja, Auth, izolacja RLS, Realtime, pliki | Logiki UI |

Granica core ↔ UI opisana w [shared-core](shared-core.md). Konsumowanie core przez klientów: [clients](clients.md). Web bez binary KMP: [ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md).

## Docelowa struktura repozytorium

Decyzja tooling / layout / wersjonowanie: [ADR 0005](../decisions/0005-monorepo-tooling.md).

```
GymManager/
  settings.gradle.kts   # Gradle multi-project: :core, :apps:android, …
  gradle/               # wrapper
  package.json          # root scripts (pnpm) — orkiestracja, nie logika
  pnpm-workspace.yaml   # apps/web (+ ewentualne paczki TS później)
  scripts/              # cienkie skrypty CI/dev (opcjonalnie)
  core/                 # KMP: domena, use-case'y, data, sync
  apps/
    ios/                # Xcode + SwiftUI; konsumuje framework z core
    android/            # Jetpack Compose; zależność Gradle na :core
    web/                # React + TS + Vite (pnpm); supabase-js — ADR 0006
  supabase/             # migracje SQL, RLS, config CLI
  docs/                 # dokumentacja i ADR (źródło prawdy decyzji)
```

### Odpowiedzialność top-level

| Ścieżka | Odpowiedzialność | Build |
|---------|------------------|-------|
| `core/` | Wspólna logika; **bez** UI | Gradle (KMP) |
| `apps/android/` | UI Compose + wiring Android | Gradle (ten sam root) |
| `apps/ios/` | UI SwiftUI + wiring iOS | Xcode; framework z Gradle/KMP |
| `apps/web/` | UI React/TS; Supabase via `supabase-js` (bez KMP-JS) | pnpm / Vite |
| `supabase/` | Schemat, RLS, lokalny/linked projekt | Supabase CLI |
| `docs/` | Wizja, architektura, ADR, konwencje | — |
| Root scripts | `build` / `test` / `lint` łączące toolchainy | pnpm / shell |

**Nie** wprowadzamy Nx/Turborepo na start. Scaffolding katalogów kodu — **faza 1** (roadmap); ten dokument opisuje docelowy układ, nie tworzy jeszcze folderów aplikacji.

## Zasady architektoniczne

1. **Jedna prawda domenowa** — reguły biznesowe żyją w core (lub w Postgres/RLS tam, gdzie izolacja / wspólne niezmienniki), nie w trzech UI.
2. **Cienkie UI** — klienci renderują i delegują; web może wołać Supabase przez `supabase-js` bez binary KMP ([ADR 0006](../decisions/0006-web-supabase-js-without-kmp-js.md)), ale **bez** równoległej domeny TS.
3. **RLS first** — izolacja danych użytkownika na poziomie bazy, nie „ufamy klientowi”.
4. **Dokumentacja przed głębokim kodem** — nowe decyzje → ADR.
5. **AI później** — żadnych założeń AI w core/infra teraz.

## Powiązane dokumenty

- [Tech stack](tech-stack.md)
- [Shared core](shared-core.md)
- [Klienci](clients.md)
- [Dane i sync](data-and-sync.md)
- [Auth](auth.md)
- ADR: [0001](../decisions/0001-shared-core-kmp.md), [0002](../decisions/0002-backend-supabase.md), [0003](../decisions/0003-native-ui-per-platform.md), [0004](../decisions/0004-supabase-hosting.md), [0005](../decisions/0005-monorepo-tooling.md), [0006](../decisions/0006-web-supabase-js-without-kmp-js.md)
