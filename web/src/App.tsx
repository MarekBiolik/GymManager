import { useCallback, useEffect, useMemo, useState, type MouseEvent } from 'react'
import { AddFoodScreen } from './AddFoodScreen'
import { deleteEntry, fetchDay, updateEntryGrams } from './api'
import { t } from './i18n'
import {
  mealMacros,
  progress,
  remaining,
  round0,
  round1,
  sumMacros,
  macrosForGrams,
} from './nutrition'
import { PortionScreen } from './PortionScreen'
import type { DayData, DiaryEntry, MealId } from './types'
import { MEAL_ORDER } from './types'
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
  const [addMeal, setAddMeal] = useState<MealId>('breakfast_1')
  const [editing, setEditing] = useState<DiaryEntry | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchDay(date)
      setDay(data)
    } catch (err) {
      setError(
        err instanceof Error ? err.message : t.app.loadDayFailed,
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

  function openAdd(meal: MealId) {
    setAddMeal(meal)
    setAdding(true)
  }

  async function handleDelete(entry: DiaryEntry, event: MouseEvent) {
    event.stopPropagation()
    try {
      await deleteEntry(entry.id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : t.app.deleteFailed)
    }
  }

  return (
    <div className="app-shell">
      <div className="phone-frame">
        <header className="top-bar">
          <h1>{t.app.today}</h1>
          <p className="date-label">{formatDateLabel(date)}</p>
        </header>

        <main className="day-scroll day-scroll--no-fab">
          {error ? <p className="error banner">{error}</p> : null}
          {loading && !day ? <p className="muted">{t.app.loading}</p> : null}

          {day ? (
            <>
              <section className="summary-card" aria-label={t.app.daySummaryAria}>
                <div className="kcal-remaining">
                  {round0(left.kcal)}
                  <span>kcal</span>
                </div>
                <p className="kcal-caption">
                  {t.app.kcalCaption(round0(day.goals.kcal), round0(totals.kcal))}
                </p>

                <div className="macro-row">
                  <div className="macro macro--protein">
                    <span className="macro-label">{t.app.protein}</span>
                    <div className="macro-value">
                      {round1(totals.protein)} / {round0(day.goals.protein)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i style={{ width: `${progress(totals.protein, day.goals.protein)}%` }} />
                    </div>
                  </div>
                  <div className="macro macro--carbs">
                    <span className="macro-label">{t.app.carbs}</span>
                    <div className="macro-value">
                      {round1(totals.carbs)} / {round0(day.goals.carbs)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i style={{ width: `${progress(totals.carbs, day.goals.carbs)}%` }} />
                    </div>
                  </div>
                  <div className="macro macro--fat">
                    <span className="macro-label">{t.app.fat}</span>
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
                const mealLabel = t.meals[meal]
                return (
                  <section key={meal} className="meal-section">
                    <div className="meal-header">
                      <div className="meal-header-text">
                        <h2>{mealLabel}</h2>
                        <p className="meal-macros">
                          {t.app.mealMacros(
                            round0(mealTotal.kcal),
                            round1(mealTotal.protein),
                            round1(mealTotal.carbs),
                            round1(mealTotal.fat),
                          )}
                        </p>
                      </div>
                      <button
                        type="button"
                        className="icon-add"
                        aria-label={t.app.addToMealAria(mealLabel)}
                        onClick={() => openAdd(meal)}
                      >
                        <span className="icon-add-glyph" aria-hidden="true">
                          +
                        </span>
                      </button>
                    </div>
                    {mealEntries.length === 0 ? (
                      <p className="meal-empty">{t.app.noProducts}</p>
                    ) : (
                      <ul className="entry-list">
                        {mealEntries.map((entry) => {
                          const m = macrosForGrams(entry.product, entry.grams)
                          return (
                            <li key={entry.id} className="entry-item">
                              <button
                                type="button"
                                className="entry-row entry-row-btn"
                                aria-label={t.app.editEntryAria(entry.product.name)}
                                onClick={() => setEditing(entry)}
                              >
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
                              </button>
                              <button
                                type="button"
                                className="entry-delete"
                                aria-label={t.app.deleteEntryAria(entry.product.name)}
                                onClick={(event) => void handleDelete(entry, event)}
                              >
                                ×
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

        <nav className="bottom-nav bottom-nav--simple" aria-label={t.app.navAria}>
          <button type="button" className="nav-btn is-active">
            {t.app.diary}
          </button>
          <button type="button" className="nav-btn" disabled>
            {t.app.more}
          </button>
        </nav>

        {adding ? (
          <AddFoodScreen
            date={date}
            meal={addMeal}
            onClose={() => setAdding(false)}
            onSaved={async () => {
              setAdding(false)
              await load()
            }}
          />
        ) : null}

        {editing ? (
          <PortionScreen
            product={editing.product}
            initialGrams={editing.grams}
            confirmLabel={t.addFood.saveChanges}
            onBack={() => setEditing(null)}
            onConfirm={async (grams) => {
              await updateEntryGrams(editing.id, grams)
              setEditing(null)
              await load()
            }}
            onDelete={async () => {
              await deleteEntry(editing.id)
              setEditing(null)
              await load()
            }}
          />
        ) : null}
      </div>
    </div>
  )
}

export default App
