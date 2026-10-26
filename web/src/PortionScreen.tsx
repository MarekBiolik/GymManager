import { useMemo, useState } from 'react'
import { t } from './i18n'
import { macrosForGrams, round0, round1 } from './nutrition'

type Nutrition = {
  name: string
  brand: string | null
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
}

type Props = {
  product: Nutrition
  initialGrams?: number
  confirmLabel: string
  onBack: () => void
  onConfirm: (grams: number) => void | Promise<void>
  onDelete?: () => void | Promise<void>
}

const PRESETS = [50, 100, 150, 200, 250]

export function PortionScreen({
  product,
  initialGrams = 100,
  confirmLabel,
  onBack,
  onConfirm,
  onDelete,
}: Props) {
  const [grams, setGrams] = useState(String(initialGrams))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const gramsNumber = Number(grams)
  const preview = useMemo(() => {
    if (!Number.isFinite(gramsNumber) || gramsNumber <= 0) return null
    return macrosForGrams(product, gramsNumber)
  }, [product, gramsNumber])

  async function confirm() {
    if (!Number.isFinite(gramsNumber) || gramsNumber <= 0) {
      setError(t.addFood.invalidGrams)
      return
    }
    setSaving(true)
    setError(null)
    try {
      await onConfirm(gramsNumber)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.addFood.saveFailed)
      setSaving(false)
    }
  }

  return (
    <div className="overlay-screen portion-screen">
      <header className="overlay-header">
        <button type="button" className="text-btn" onClick={onBack}>
          {t.addFood.back}
        </button>
        <h1>{t.addFood.portionTitle}</h1>
        <span className="header-spacer" />
      </header>

      <div className="overlay-body">
        <div className="portion-product">
          <strong>{product.name}</strong>
          {product.brand ? <span className="muted"> · {product.brand}</span> : null}
          <div className="muted small">
            {round0(product.kcal_100g)} kcal · B {round1(product.protein_100g)} · W{' '}
            {round1(product.carbs_100g)} · T {round1(product.fat_100g)} / 100 g
          </div>
        </div>

        <label className="field">
          <span>{t.addFood.portionGrams}</span>
          <input
            inputMode="decimal"
            value={grams}
            onChange={(e) => setGrams(e.target.value)}
            autoFocus
          />
        </label>

        <div className="portion-presets">
          {PRESETS.map((preset) => (
            <button
              key={preset}
              type="button"
              className={
                Number(grams) === preset
                  ? 'portion-preset is-active'
                  : 'portion-preset'
              }
              onClick={() => setGrams(String(preset))}
            >
              {preset} g
            </button>
          ))}
        </div>

        {preview ? (
          <p className="portion-preview">
            {round0(preview.kcal)} kcal · B {round1(preview.protein)} · W{' '}
            {round1(preview.carbs)} · T {round1(preview.fat)}
          </p>
        ) : null}

        {error ? <p className="error">{error}</p> : null}

        <button
          type="button"
          className="primary-btn"
          disabled={saving}
          onClick={() => void confirm()}
        >
          {saving ? t.addFood.saving : confirmLabel}
        </button>

        {onDelete ? (
          <button
            type="button"
            className="text-btn danger delete-entry-btn"
            disabled={saving}
            onClick={() => void onDelete()}
          >
            {t.app.delete}
          </button>
        ) : null}
      </div>
    </div>
  )
}
