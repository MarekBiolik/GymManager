# ADR 0005 — Monorepo: layout, tooling i wersjonowanie core

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja zamykania OQ-04)

## Kontekst

Repozytorium ma trzymać **jeden produkt** na trzech klientach (iOS, Android, web) ze wspólnym core KMP ([ADR 0001](0001-shared-core-kmp.md), [ADR 0003](0003-native-ui-per-platform.md)). Trzeba ustalić:

1. jak dzielimy katalogi i buildy (Gradle vs Node),
2. czy dodajemy osobny orchestrator monorepo (Nx / Turborepo),
3. jak wersjonujemy `core` względem klientów (store builds, web deploy).

Złe narzędzie = podwójne systemy buildu bez jasnej granicy albo przedwczesna złożoność (Nx na trzy ekosystemy). Brak decyzji o wersjonowaniu utrudnia CI i release w fazie 1/6.

## Decyzja

**Hybrydowe monorepo bez Nx/Turborepo:**

1. **Gradle multi-project** obejmuje `core/` (KMP) oraz `apps/android/` (Compose). iOS konsumuje artefakt KMP (XCFramework / lokalny SPM) budowany z tego samego Gradle; projekt Xcode żyje w `apps/ios/`.
2. **`apps/web/`** to osobny workspace **pnpm** + Vite + React/TS — poza drzewem Gradle. Integracja z core zależy od [OQ-01](../open-questions.md); do czasu zamknięcia OQ-01 web nie publikuje własnej logiki domenowej „na skróty”.
3. **Root** trzyma cienkie skrypty orkiestracji (`package.json` / Makefile / skrypty w `scripts/`) wywołujące Gradle, pnpm i CLI Supabase — bez drugiego „meta-buildera”.
4. **Wersjonowanie `core`:** do czasu osobnej publikacji biblioteki core jest **source-linked w tym samym commicie** co klienci (zależność projektowa Gradle / lokalny framework). SemVer dla core + ewentualna publikacja Maven/SPM — dopiero gdy pojawi się potrzeba zewnętrznego konsumenta lub niezależnego cyklu release; do tego wystarczy plik `core/VERSION` (lub `gradle.properties`) synchronizowany przy tagach release produktu. Numery buildów store (Android `versionCode`, iOS `CFBundleVersion`) i deploy web są **niezależne** od SemVer core.

Docelowe drzewo i odpowiedzialności: [overview](../architecture/overview.md). Stack narzędzi: [tech-stack](../architecture/tech-stack.md).

## Alternatywy rozważone

1. **Jedno mega-Gradle (w tym web przez plugin)** — jeden tool. Odrzucone: webowy DX (Vite, ESLint, pnpm) słabo pasuje do Gradle; koszt utrzymania pluginów bez zysku.
2. **Nx / Turborepo nad całym repo** — cache tasków, graf zależności. Odrzucone na start: trzy toolchainy (Gradle, Xcode, Node); Nx/Turbo realnie pomagają głównie w JS. Można rozważyć **później tylko dla `apps/web` + paczek TS**, jeśli web urośnie.
3. **Polyrepo** (osobne repo na core / każdy klient) — czyste granice wersji. Odrzucone: solo/mały zespół, częste zmiany cross-cutting, ADR i scaffolding w jednym miejscu są ważniejsze niż niezależne release'y.
4. **Publikacja core do Maven Central / prywatnego rejestru od dnia 1** — twarde SemVer. Odrzucone jako przedwczesne: jeden produkt, jeden commit; source-link jest prostszy do CI i lokalnego DX.
5. **Hybryda Gradle (core+Android) + pnpm (web) + root scripts** — wybrane: naturalne dla KMP/Android, standardowe dla React, minimalna orkiestracja.

## Konsekwencje

- Pozytywne: jasny podział toolchainów; Android i core dzielą jeden Gradle; web zachowuje dojrzały DX; root scripts wystarczą do „build all” / CI; brak vendor lock-in w Nx.
- Negatywne / koszty: dwa (trzy z Xcode) systemy buildu do nauczenia; skrypty root trzeba utrzymywać ręcznie; iOS wymaga kroku eksportu frameworka z Gradle przed/obok Xcode.
- Później: OQ-01 (jak web bierze core — artifact JS/Wasm vs inne); playbook scaffoldu ([issue #7](https://github.com/MarekBiolik/GymManager/issues/7)); SemVer/publikacja core tylko gdy zajdzie potrzeba; ewentualnie Turborepo wyłącznie pod web.

### Scope pierwszej implementacji scaffold (faza 1) vs później

| W scope pierwszego scaffoldu | Świadomie później |
|------------------------------|-------------------|
| Root: `settings.gradle.kts` / wrapper, skrypty `build`/`test`/`lint` | Nx / Turborepo |
| `core/` jako moduł KMP (Android + iOS targets; JS/Wasm wg OQ-01) | Publikacja core do rejestru zewnętrznego |
| `apps/android/` zależny od `:core` | Niezależne wersjonowanie core vs klientów |
| `apps/ios/` Xcode project + instrukcja podpięcia frameworka | Pełny SPM package distribution |
| `apps/web/` pnpm + Vite (UI skeleton; wiring core po OQ-01) | Monorepo JS z wieloma paczkami |
| `supabase/` CLI config + miejsce na migracje | Multi-environment release automation |
| `docs/` (już istnieje) | — |
| `core/VERSION` lub wpis w `gradle.properties` (placeholder) | Store release / tag policy (faza 6) |

## Powiązane

- [overview — struktura repo](../architecture/overview.md)
- [tech-stack](../architecture/tech-stack.md)
- [shared-core](../architecture/shared-core.md)
- [clients](../architecture/clients.md)
- [OQ-04](../open-questions.md) (decided)
- [ADR 0001](0001-shared-core-kmp.md), [ADR 0003](0003-native-ui-per-platform.md)
- [roadmap — faza 1](../roadmap.md)
