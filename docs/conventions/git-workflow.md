# Git workflow

## Gałęzie

| Gałąź | Rola |
|-------|------|
| `master` (lub później `main`) | Stabilna linia; chroniona gdy pojawi się CI |
| `feat/…`, `fix/…`, `docs/…`, `chore/…` | Krótkie gałęzie robocze |

Na start solo: krótkie gałęzie + PR do `master` nawet przy jednym autorze — historia i review docs zostają czytelne.

## Commity

- Impersonalny tryb rozkazujący, po angielsku lub po polsku — **wybierz jeden styl i trzymaj się go** (rekomendacja: angielski, konwencja Conventional Commits).

Przykłady:

```
docs: add architecture overview and ADRs 0001-0003
feat(core): add FoodEntry validation
fix(auth): refresh session before sync
chore: remove prototype web/dist artifacts
```

- Jedna intencja na commit.
- Nie commituj `node_modules`, buildów, lokalnych `.env`, baz z danymi osobowymi.

## Pull requesty

- Opis: **dlaczego** + jak przetestować.
- Checklist: czy potrzebny ADR / update `docs/` / migracja RLS.
- Nie merguj tajemnic ani wygenerowanych artefaktów.

## Tagowanie / release

Po pojawieniu się aplikacji: semver dla `core` i store build numbers dla klientów — szczegóły przy fazie 1/6 (OQ-04).

## Zakazy

- `git push --force` na `master` bez jawnej decyzji.
- Commit `--no-verify` bez powodu.
- Amend wypchniętego commitu bez uzgodnienia.
