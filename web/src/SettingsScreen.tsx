import { useState, type FormEvent } from 'react'
import { updateGoals } from './api'
import { t } from './i18n'
import type { Goals } from './types'

type Props = {
  goals: Goals
  onClose: () => void
  onSaved: (goals: Goals) => void
}

export function SettingsScreen({ goals, onClose, onSaved }: Props) {
  const [kcal, setKcal] = useState(String(goals.kcal))
  const [protein, setProtein] = useState(String(goals.protein))
  const [carbs, setCarbs] = useState(String(goals.carbs))
  const [fat, setFat] = useState(String(goals.fat))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const next: Goals = {
      kcal: Number(kcal),
      protein: Number(protein),
      carbs: Number(carbs),
      fat: Number(fat),
    }

    if (
      ![next.kcal, next.protein, next.carbs, next.fat].every((n) =>
        Number.isFinite(n),
      ) ||
      next.kcal <= 0 ||
      next.protein < 0 ||
      next.carbs < 0 ||
      next.fat < 0
    ) {
      setError(t.settings.invalidGoals)
      return
    }

    setSaving(true)
    setError(null)
    try {
      const saved = await updateGoals(next)
      onSaved(saved)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.settings.saveFailed)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="overlay-screen">
      <header className="overlay-header">
        <button type="button" className="text-btn" onClick={onClose}>
          {t.settings.close}
        </button>
        <h1>{t.settings.title}</h1>
        <span className="header-spacer" />
      </header>

      <form className="overlay-body custom-form" onSubmit={onSubmit}>
        <p className="muted">{t.settings.goalsHint}</p>

        <div className="macro-grid">
          <label className="field">
            <span>{t.settings.kcal}</span>
            <input
              inputMode="numeric"
              value={kcal}
              onChange={(e) => setKcal(e.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>{t.settings.protein}</span>
            <input
              inputMode="decimal"
              value={protein}
              onChange={(e) => setProtein(e.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>{t.settings.carbs}</span>
            <input
              inputMode="decimal"
              value={carbs}
              onChange={(e) => setCarbs(e.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>{t.settings.fat}</span>
            <input
              inputMode="decimal"
              value={fat}
              onChange={(e) => setFat(e.target.value)}
              required
            />
          </label>
        </div>

        {error ? <p className="error">{error}</p> : null}

        <button type="submit" className="primary-btn" disabled={saving}>
          {saving ? t.settings.saving : t.settings.save}
        </button>
      </form>
    </div>
  )
}
