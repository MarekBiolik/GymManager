# Testowanie

## Priorytet

1. **Unit testy w `core`** — reguły domenowe, walidacje, use-case'y (najwyższy ROI).
2. **Testy migracji / RLS** — polityki nie wpuszczają cudzych wierszy (SQL lub testy integracyjne Supabase).
3. **Testy UI** — selektywnie na krytyczne flow (login, zapis wpisu); nie na każdy ekran na start.

## Zasady

- Nową regułę biznesową w core = test w tym samym PR.
- Testy nie wołają produkcyjnego Supabase; używamy lokalnego / mocków / project staging.
- Flaky test = bug; nie ignorujemy `skip` na stałe bez issue.

## Per warstwa (kierunek narzędzi)

| Warstwa | Kierunek |
|---------|----------|
| core (KMP) | `kotlin.test` / JUnit w commonTest |
| Android UI | Compose testing — później |
| iOS UI | XCTest / snapshot — później |
| Web | Vitest + Testing Library — później |
| RLS | Testy z dwoma JWT użytkowników |

## Coverage

Nie ustawiamy sztywnego % na start. Wymagamy testów dla: auth session handling, sync conflict helpers (gdy powstaną), wyliczeń makro/agregacji domenowych.

## Manualne

Przed release sklepu: smoke na realnym urządzeniu iOS + Android + przeglądarka (login, CRUD jednej encji, wylogowanie).
