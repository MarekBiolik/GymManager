# Słownik domenowy

Jednoznaczne nazwy używane w docs, ADR, kodzie i schemacie. Nowe pojęcia dopisujemy tutaj **zanim** rozjadą się synonimy w PR.

## Produkt

| Termin | Znaczenie |
|--------|-----------|
| **GymManager** | Nazwa projektu / super-aplikacji treningowej |
| **Moduł** | Obszar produktu w jednej apce: żywienie, trening, pomiary |
| **Konto** | Tożsamość Supabase Auth; służy do sync i izolacji danych, nie do profilu społecznościowego |
| **IdP** | Identity Provider — tu Google oraz Sign in with Apple (iOS); decyzja: [ADR 0007](../decisions/0007-auth-providers-minimum-pii.md) |
| **PII** | Dane osobowe; w GymManager zbieramy minimum (email tylko w Auth z IdP, nie w `profiles`) |

## Żywienie

| Termin | Znaczenie |
|--------|-----------|
| **Food** | Produkt lub przepis zdefiniowany przez użytkownika (lub zaimportowany), z makro na porcję/100 g |
| **Food entry** | Wpis spożycia: co, ile, kiedy (powiązany z Food lub snapshotem wartości) |
| **Diary / dziennik** | Widok wpisów w dniu (UI); niekoniecznie osobna tabela |
| **Macro** | Białko, węglowodany, tłuszcz (i kalorie pochodne) |

## Trening

| Termin | Znaczenie |
|--------|-----------|
| **Exercise** | Ćwiczenie (np. przysiad); szablon, nie wykonanie |
| **Workout / Session** | Konkretna sesja treningowa w czasie |
| **Set** | Seria w ramach sesji (powtórzenia, ciężar, RPE… — pola do precyzacji) |
| **Template** | Opcjonalny plan/szablon treningu do ponownego użycia (później) |

## Pomiary

| Termin | Znaczenie |
|--------|-----------|
| **Measurement** | Pomiar ciała w czasie (waga, obwody…); typ + wartość + timestamp |
| **Metric type** | Rodzaj pomiaru (np. `body_weight_kg`) |

## Techniczne (używane w domenie danych)

| Termin | Znaczenie |
|--------|-----------|
| **Core** | Wspólny moduł KMP z logiką domenową |
| **RLS** | Row Level Security — izolacja wierszy per `user_id` |
| **Sync** | Uzgodnienie stanu lokalnego z Postgresem |
| **Source of truth** | Po sync: Postgres; lokalny cache jest wtórny |

## Unikać

- Mylenia **Food** (definicja) z **Food entry** (zdarzenie spożycia).
- Synonimów „trening / workout / sesja” bez precyzji — w schemacie preferujemy ustalone nazwy tabel z [data-model](data-model.md).
- „User profile” w sensie social — mamy co najwyżej `profiles` techniczne.
