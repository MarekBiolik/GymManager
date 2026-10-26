import './App.css'

const MEALS = [
  { id: 'breakfast', label: 'Śniadanie' },
  { id: 'lunch', label: 'Obiad' },
  { id: 'dinner', label: 'Kolacja' },
  { id: 'snacks', label: 'Przekąski' },
] as const

function formatToday(): string {
  return new Intl.DateTimeFormat('pl-PL', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date())
}

function App() {
  const dateLabel = formatToday()

  return (
    <div className="app-shell">
      <div className="phone-frame">
        <header className="top-bar">
          <h1>Dziś</h1>
          <p className="date-label">{dateLabel}</p>
        </header>

        <main className="day-scroll">
          <section className="summary-card" aria-label="Podsumowanie dnia">
            <div className="kcal-remaining">
              —<span>kcal</span>
            </div>
            <p className="kcal-caption">pozostało z celu</p>

            <div className="macro-row">
              <div className="macro macro--protein">
                <span className="macro-label">Białko</span>
                <div className="macro-value">— / — g</div>
                <div className="macro-bar" aria-hidden="true">
                  <i />
                </div>
              </div>
              <div className="macro macro--carbs">
                <span className="macro-label">Węgle</span>
                <div className="macro-value">— / — g</div>
                <div className="macro-bar" aria-hidden="true">
                  <i />
                </div>
              </div>
              <div className="macro macro--fat">
                <span className="macro-label">Tłuszcz</span>
                <div className="macro-value">— / — g</div>
                <div className="macro-bar" aria-hidden="true">
                  <i />
                </div>
              </div>
            </div>
          </section>

          {MEALS.map((meal) => (
            <section key={meal.id} className="meal-section">
              <div className="meal-header">
                <h2>{meal.label}</h2>
                <span className="meal-kcal">0 kcal</span>
              </div>
              <p className="meal-empty">Brak produktów</p>
            </section>
          ))}
        </main>

        <nav className="bottom-nav" aria-label="Nawigacja">
          <button type="button" className="nav-btn is-active">
            Dziennik
          </button>
          <button type="button" className="add-fab" aria-label="Dodaj jedzenie" disabled>
            +
          </button>
          <button type="button" className="nav-btn" disabled>
            Więcej
          </button>
        </nav>
      </div>
    </div>
  )
}

export default App
