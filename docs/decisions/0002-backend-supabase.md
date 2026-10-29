# ADR 0002 — Backend: Supabase

- **Status:** accepted
- **Data:** 2026-10-29
- **Decydenci:** Marek (+ sesja planowania architektury)

## Kontekst

Potrzebujemy: miejsca na dane apki (historia rok+), sync między urządzeniami, bezpiecznego logowania przy **minimum utrzymania** i **minimum PII**. Tracker generuje zapytania agregujące (sumy, trendy) — model relacyjny pasuje lepiej niż dokumentowy. Zespół/solo nie chce na start utrzymywać własnego serwera aplikacji.

## Decyzja

Używamy **Supabase**: PostgreSQL + Auth + Row Level Security + Realtime + Storage. Klienci / core łączą się przez oficjalne / community SDK (`supabase-kt`, `supabase-js`). Hosting Cloud Free → Pro, środowiska i backupy: [ADR 0004](0004-supabase-hosting.md).

## Alternatywy rozważone

1. **Firebase (Auth + Firestore)** — szybki start, Google login „native” dla ekosystemu Google. Odrzucone: silniejszy **vendor lock-in** (własne API/model); **słabsze agregacje** NoSQL dla historii/wykresów trackera.
2. **Własny backend Node/TS + Postgres + biblioteka auth** — pełna kontrola. Odrzucone na start: najwyższy koszt utrzymania (serwer, patching, auth hardening, backupy).
3. **Reuse starego serwera prototypu** — źródeł brak na dysku; byłby de facto wariant „własny backend”.
4. **Supabase** — wybrany: Postgres (agregacje, migracje SQL), Auth z Google/Apple, RLS jako izolacja użytkownika, możliwość eksportu/self-host (słabszy lock-in niż Firestore).

### Słownik (dla kontekstu decyzji)

- **Lock-in:** koszt ucieczki od dostawcy. Firestore = przepisywanie dostępu do danych; Supabase = standardowy Postgres (dump/restore na inny hosting).
- **Agregacja:** zapytania typu „suma kalorii dziennie przez rok”. SQL robi to naturalnie; Firestore często wymaga denormalizacji i kosztów odczytów.

## Konsekwencje

- Pozytywne: mało własnej infra; RLS egzekwuje „dane tylko właściciela”; darmowy tier na start; ścieżka wyjścia przez SQL dump.
- Negatywne: zależność od produktu Supabase Auth/API; community `supabase-kt` wymaga monitorowania dojrzałości; trzeba dyscypliny migracji + RLS przy każdej tabeli.
- Później: OQ-03 (sync). Hosting/backupy zamknięte w [ADR 0004](0004-supabase-hosting.md). Metoda logowania / PII: [ADR 0007](0007-auth-providers-minimum-pii.md).

## Powiązane

- [data-and-sync](../architecture/data-and-sync.md)
- [auth](../architecture/auth.md)
- [tech-stack](../architecture/tech-stack.md)
