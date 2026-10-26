import { useCallback, useEffect, useMemo, useState } from 'react'
import { AddFoodScreen } from './AddFoodScreen'
import { deleteEntry, fetchDay } from './api'
import {
  mealMacros,
  progress,
  remaining,
  round0,
  round1,
  sumMacros,
  macrosForGrams,
} from './nutrition'
import type { DayData, MealId } from './types'
import { MEAL_LABELS, MEAL_ORDER } from './types'
import './App.css'

function todayLocal(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatDateLabel(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Intl.DateTimeFormat('pl-PL', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date(y, m - 1, d))
}

function App() {
  const date = todayLocal()
  const [day, setDay] = useState<DayData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)
  const [addMeal, setAddMeal] = useState<MealId>('breakfast')

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchDay(date)
      setDay(data)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Nie udało się wczytać dnia. Uruchom serwer API.',
      )
    } finally {
      setLoading(false)
    }
  }, [date])

  useEffect(() => {
    void load()
  }, [load])

  const totals = useMemo(
    () => (day ? sumMacros(day.entries) : { kcal: 0, protein: 0, carbs: 0, fat: 0 }),
    [day],
  )

  const left = useMemo(
    () =>
      day
        ? remaining(day.goals, totals)
        : { kcal: 0, protein: 0, carbs: 0, fat: 0 },
    [day, totals],
  )

  async function handleDelete(id: number) {
    try {
      await deleteEntry(id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Nie udało się usunąć')
    }
  }

  function openAdd(meal: MealId) {
    setAddMeal(meal)
    setAdding(true)
  }

  return (
    <div className="app-shell">
      <div className="phone-frame">
        <header className="top-bar">
          <h1>Dziś</h1>
          <p className="date-label">{formatDateLabel(date)}</p>
        </header>

        <main className="day-scroll">
          {error ? <p className="error banner">{error}</p> : null}
          {loading && !day ? <p className="muted">Ładowanie…</p> : null}

          {day ? (
            <>
              <section className="summary-card" aria-label="Podsumowanie dnia">
                <div className="kcal-remaining">
                  {round0(left.kcal)}
                  <span>kcal</span>
                </div>
                <p className="kcal-caption">
                  pozostało z {round0(day.goals.kcal)} · zjedzono {round0(totals.kcal)}
                </p>

                <div className="macro-row">
                  <div className="macro macro--protein">
                    <span className="macro-label">Białko</span>
                    <div className="macro-value">
                      {round1(totals.protein)} / {round0(day.goals.protein)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i style={{ width: `${progress(totals.protein, day.goals.protein)}%` }} />
                    </div>
                  </div>
                  <div className="macro macro--carbs">
                    <span className="macro-label">Węgle</span>
                    <div className="macro-value">
                      {round1(totals.carbs)} / {round0(day.goals.carbs)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i style={{ width: `${progress(totals.carbs, day.goals.carbs)}%` }} />
                    </div>
                  </div>
                  <div className="macro macro--fat">
                    <span className="macro-label">Tłuszcz</span>
                    <div className="macro-value">
                      {round1(totals.fat)} / {round0(day.goals.fat)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i style={{ width: `${progress(totals.fat, day.goals.fat)}%` }} />
                    </div>
                  </div>
                </div>
              </section>

              {MEAL_ORDER.map((meal) => {
                const mealEntries = day.entries.filter((entry) => entry.meal === meal)
                const mealTotal = mealMacros(day.entries, meal)
                return (
                  <section key={meal} className="meal-section">
                    <div className="meal-header">
                      <h2>{MEAL_LABELS[meal]}</h2>
                      <div className="meal-header-right">
                        <span className="meal-kcal">{round0(mealTotal.kcal)} kcal</span>
                        <button
                          type="button"
                          className="icon-add"
                          aria-label={`Dodaj do: ${MEAL_LABELS[meal]}`}
                          onClick={() => openAdd(meal)}
                        >
                          +
                        </button>
                      </div>
                    </div>
                    {mealEntries.length === 0 ? (
                      <p className="meal-empty">Brak produktów</p>
                    ) : (
                      <ul className="entry-list">
                        {mealEntries.map((entry) => {
                          const m = macrosForGrams(entry.product, entry.grams)
                          return (
                            <li key={entry.id} className="entry-row">
                              <div>
                                <strong>{entry.product.name}</strong>
                                {entry.product.brand ? (
                                  <span className="muted"> · {entry.product.brand}</span>
                                ) : null}
                                <div className="muted small">
                                  {round0(entry.grams)} g · {round0(m.kcal)} kcal · B{' '}
                                  {round1(m.protein)} · W {round1(m.carbs)} · T{' '}
                                  {round1(m.fat)}
                                </div>
                              </div>
                              <button
                                type="button"
                                className="text-btn danger"
                                onClick={() => void handleDelete(entry.id)}
                              >
                                Usuń
                              </button>
                            </li>
                          )
                        })}
                      </ul>
                    )}
                  </section>
                )
              })}
            </>
          ) : null}
        </main>

        <nav className="bottom-nav" aria-label="Nawigacja">
          <button type="button" className="nav-btn is-active">
            Dziennik
          </button>
          <button
            type="button"
            className="add-fab"
            aria-label="Dodaj jedzenie"
            onClick={() => openAdd('breakfast')}
          >
            +
          </button>
          <button type="button" className="nav-btn" disabled>
            Więcej
          </button>
        </nav>

        {adding ? (
          <AddFoodScreen
            date={date}
            initialMeal={addMeal}
            onClose={() => setAdding(false)}
            onSaved={async () => {
              setAdding(false)
              await load()
            }}
          />
        ) : null}
      </div>
    </div>
  )
}

export default App
