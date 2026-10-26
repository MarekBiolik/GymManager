import { useCallback, useEffect, useMemo, useState, type MouseEvent } from 'react'
import { AddFoodScreen } from './AddFoodScreen'
import {
  deleteEntry,
  fetchDay,
  fetchWeek,
  updateEntryGrams,
  type WeekDaySummary,
} from './api'
import {
  formatWeekRangeLabel,
  isToday,
  parseIsoDate,
  shiftIsoDate,
  startOfWeekMonday,
  todayLocal,
  weekDates,
  WEEKDAY_SHORT_PL,
} from './dates'
import { t } from './i18n'
import {
  mealMacros,
  progress,
  round0,
  round1,
  sumMacros,
  macrosForGrams,
} from './nutrition'
import { PortionScreen } from './PortionScreen'
import { SettingsScreen } from './SettingsScreen'
import type { DayData, DiaryEntry, MealId } from './types'
import { MEAL_ORDER } from './types'
import './App.css'

function App() {
  const [date, setDate] = useState(todayLocal)
  const [day, setDay] = useState<DayData | null>(null)
  const [weekDays, setWeekDays] = useState<WeekDaySummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [adding, setAdding] = useState(false)
  const [addMeal, setAddMeal] = useState<MealId>('breakfast_1')
  const [editing, setEditing] = useState<DiaryEntry | null>(null)

  const weekStart = useMemo(() => startOfWeekMonday(date), [date])
  const weekStrip = useMemo(() => weekDates(date), [date])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [dayData, weekData] = await Promise.all([
        fetchDay(date),
        fetchWeek(weekStart),
      ])
      setDay(dayData)
      setWeekDays(weekData.days)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.app.loadDayFailed)
    } finally {
      setLoading(false)
    }
  }, [date, weekStart])

  useEffect(() => {
    void load()
  }, [load])

  const totals = useMemo(
    () => (day ? sumMacros(day.entries) : { kcal: 0, protein: 0, carbs: 0, fat: 0 }),
    [day],
  )

  const weekByDate = useMemo(() => {
    const map = new Map<string, WeekDaySummary>()
    for (const item of weekDays) map.set(item.date, item)
    return map
  }, [weekDays])

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
        <header className="week-header">
          <div className="week-toolbar">
            <button
              type="button"
              className="day-nav-btn"
              aria-label={t.app.prevWeek}
              onClick={() => setDate((d) => shiftIsoDate(startOfWeekMonday(d), -7))}
            >
              ‹
            </button>
            <div className="week-heading">
              <p className="week-range">{formatWeekRangeLabel(date)}</p>
              {!isToday(date) ? (
                <button
                  type="button"
                  className="text-btn jump-today"
                  onClick={() => setDate(todayLocal())}
                >
                  {t.app.jumpToToday}
                </button>
              ) : null}
            </div>
            <button
              type="button"
              className="day-nav-btn"
              aria-label={t.app.nextWeek}
              onClick={() => setDate((d) => shiftIsoDate(startOfWeekMonday(d), 7))}
            >
              ›
            </button>
          </div>

          <div className="week-strip" role="tablist" aria-label={t.app.weekAria}>
            {weekStrip.map((iso, index) => {
              const dayNum = parseIsoDate(iso).getDate()
              const label = WEEKDAY_SHORT_PL[index]
              const selected = iso === date
              const today = isToday(iso)
              const summary = weekByDate.get(iso)
              return (
                <button
                  key={iso}
                  type="button"
                  role="tab"
                  aria-selected={selected}
                  aria-label={t.app.selectDayAria(label, dayNum)}
                  className={[
                    'week-day',
                    selected ? 'is-selected' : '',
                    today ? 'is-today' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => setDate(iso)}
                >
                  <span className="week-day-label">{label}</span>
                  <span className="week-day-num">{dayNum}</span>
                  <span
                    className={
                      summary?.hasEntries ? 'week-day-dot is-on' : 'week-day-dot'
                    }
                    aria-hidden="true"
                  />
                </button>
              )
            })}
          </div>
        </header>

        <main className="day-scroll day-scroll--no-fab">
          {error ? <p className="error banner">{error}</p> : null}
          {loading && !day ? <p className="muted">{t.app.loading}</p> : null}

          {day ? (
            <>
              <section className="summary-card" aria-label={t.app.daySummaryAria}>
                <div className="macro macro--kcal summary-kcal">
                  <span className="macro-label">{t.app.kcal}</span>
                  <div className="macro-value macro-value--lg">
                    {round0(totals.kcal)} / {round0(day.goals.kcal)}
                  </div>
                  <div className="macro-bar macro-bar--lg" aria-hidden="true">
                    <i style={{ width: `${progress(totals.kcal, day.goals.kcal)}%` }} />
                  </div>
                </div>

                <div className="macro-row">
                  <div className="macro macro--protein">
                    <span className="macro-label">{t.app.protein}</span>
                    <div className="macro-value">
                      {round1(totals.protein)} / {round0(day.goals.protein)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i
                        style={{
                          width: `${progress(totals.protein, day.goals.protein)}%`,
                        }}
                      />
                    </div>
                  </div>
                  <div className="macro macro--carbs">
                    <span className="macro-label">{t.app.carbs}</span>
                    <div className="macro-value">
                      {round1(totals.carbs)} / {round0(day.goals.carbs)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i
                        style={{
                          width: `${progress(totals.carbs, day.goals.carbs)}%`,
                        }}
                      />
                    </div>
                  </div>
                  <div className="macro macro--fat">
                    <span className="macro-label">{t.app.fat}</span>
                    <div className="macro-value">
                      {round1(totals.fat)} / {round0(day.goals.fat)} g
                    </div>
                    <div className="macro-bar" aria-hidden="true">
                      <i
                        style={{
                          width: `${progress(totals.fat, day.goals.fat)}%`,
                        }}
                      />
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
          <button
            type="button"
            className={settingsOpen ? 'nav-btn' : 'nav-btn is-active'}
            onClick={() => setSettingsOpen(false)}
          >
            {t.app.diary}
          </button>
          <button
            type="button"
            className={settingsOpen ? 'nav-btn is-active' : 'nav-btn'}
            onClick={() => setSettingsOpen(true)}
          >
            {t.app.settings}
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

        {settingsOpen && day ? (
          <SettingsScreen
            goals={day.goals}
            onClose={() => setSettingsOpen(false)}
            onSaved={(goals) => {
              setDay((prev) => (prev ? { ...prev, goals } : prev))
              setSettingsOpen(false)
            }}
          />
        ) : null}
      </div>
    </div>
  )
}

export default App
