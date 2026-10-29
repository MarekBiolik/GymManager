# CI/CD

Szkielet zasad. Konkrety pipeline (GitHub Actions vs inne) — faza 1.

## Cele CI na PR

1. Build `core` (przynajmniej target Android/JVM; iOS gdy runner dostępny).
2. Lint / format check (Kotlin, TS; Swift gdy możliwe).
3. Unit testy `core`.
4. Walidacja migracji Supabase (dry-run / `supabase db lint` gdy skonfigurowane).
5. Build web (gdy `apps/web` istnieje).

PR nie przechodzi przy padającym buildzie core lub testach.

## CD (kierunek)

| Artefakt | Dystrybucja |
|----------|-------------|
| Web | Hosting statyczny (np. Cloudflare Pages / Vercel / Supabase hosting) — wybór później |
| Android | Play Console (wewnętrzny track najpierw) |
| iOS | TestFlight → App Store (Apple Developer) |
| Supabase | Migracje aplikowane na staging, potem prod; nigdy „klik w dashboard” jako źródło prawdy |

## Sekrety

- W CI: secrets store (np. GitHub Secrets).
- Rozdział `dev` / `staging` / `prod` project Supabase — docelowo (OQ-05).
- Żadnych sekretów w logach builda.

## OTA / hotfix

Na razie brak. Ewentualne szybkie poprawki web łatwiejsze niż mobile (review store).

## Definition of done dla pipeline (faza 1)

- [ ] Workflow na PR: lint + test core + build web (placeholder OK jeśli web pusty)
- [ ] Branch protection na `master` gdy CI istnieje
- [ ] Dokumentacja jak odpalić te same checki lokalnie
