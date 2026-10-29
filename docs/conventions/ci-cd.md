# CI/CD

Szkielet zasad. Konkrety pipeline (GitHub Actions vs inne) — faza 1.

## Cele CI na PR

Zgodnie z [ADR 0005](../decisions/0005-monorepo-tooling.md) pipeline łączy **osobne toolchainy** (Gradle, pnpm, ewentualnie Xcode), nie jeden orchestrator monorepo.

1. Build `core` przez Gradle (przynajmniej target Android/JVM; iOS / XCFramework gdy runner dostępny).
2. Lint / format check (Kotlin via Gradle; TS via pnpm; Swift gdy możliwe).
3. Unit testy `core` (Gradle).
4. Walidacja migracji Supabase (dry-run / `supabase db lint` gdy skonfigurowane).
5. Build web przez pnpm (gdy `apps/web` istnieje).

PR nie przechodzi przy padającym buildzie core lub testach. Root scripts powinny wystawiać te same komendy lokalnie i w CI.

## CD (kierunek)

| Artefakt | Dystrybucja |
|----------|-------------|
| Web | Hosting statyczny (np. Cloudflare Pages / Vercel / Supabase hosting) — wybór później |
| Android | Play Console (wewnętrzny track najpierw) |
| iOS | TestFlight → App Store (Apple Developer) |
| Supabase | Migracje aplikowane na staging, potem prod; nigdy „klik w dashboard” jako źródło prawdy |

## Sekrety

- W CI: secrets store (np. GitHub Secrets).
- Na start: jeden projekt Supabase Cloud = `dev` ([ADR 0004](../decisions/0004-supabase-hosting.md)). Osobne `staging` / `prod` przy publicznej becie.
- `service_role` tylko w Secrets / lokalnym adminie — nigdy w klientach ani logach.
- Żadnych sekretów w logach builda.

## OTA / hotfix

Na razie brak. Ewentualne szybkie poprawki web łatwiejsze niż mobile (review store).

## Definition of done dla pipeline (faza 1)

- [ ] Workflow na PR: lint + test core + build web (placeholder OK jeśli web pusty)
- [ ] Branch protection na `master` gdy CI istnieje
- [ ] Dokumentacja jak odpalić te same checki lokalnie
