import { useMemo, useState } from 'react'
import { t } from './i18n'
import { macrosForGrams, round0, round1 } from './nutrition'
import { productServings, type ServingOption } from './types'

type Nutrition = {
  name: string
  brand: string | null
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  servings?: ServingOption[] | null
}

type Props = {
  product: Nutrition
  initialGrams?: number
  confirmLabel: string
  onBack: () => void
  onConfirm: (grams: number) => void | Promise<void>
  onDelete?: () => void | Promise<void>
}

function pickInitialServing(
  servings: ServingOption[],
  initialGrams?: number,
): ServingOption {
  if (initialGrams != null) {
    const exact = servings.find((s) => s.grams === initialGrams)
    if (exact) return exact
  }
  return servings.find((s) => s.key !== 'g') ?? servings[0]
}

export function PortionScreen({
  product,
  initialGrams = 100,
  confirmLabel,
  onBack,
  onConfirm,
  onDelete,
}: Props) {
  const servings = productServings(product)
  const [servingKey, setServingKey] = useState(
    () => pickInitialServing(servings, initialGrams).key,
  )
  const activeServing =
    servings.find((s) => s.key === servingKey) ?? servings[0]
  const isGramsUnit = activeServing.grams === 1 && activeServing.key === 'g'

  const [amount, setAmount] = useState(() => {
    const initial = pickInitialServing(servings, initialGrams)
    if (initial.grams === 1 && initial.key === 'g') {
      return String(initialGrams)
    }
    if (initialGrams && initial.grams > 1) {
      const qty = initialGrams / initial.grams
      return Number.isInteger(qty) ? String(qty) : qty.toFixed(1)
    }
    return '1'
  })

  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const amountNumber = Number(amount.replace(',', '.'))
  const gramsNumber = useMemo(() => {
    if (!Number.isFinite(amountNumber) || amountNumber <= 0) return NaN
    return amountNumber * activeServing.grams
  }, [amountNumber, activeServing.grams])

  const preview = useMemo(() => {
    if (!Number.isFinite(gramsNumber) || gramsNumber <= 0) return null
    return macrosForGrams(product, gramsNumber)
  }, [product, gramsNumber])

  function selectServing(option: ServingOption) {
    if (Number.isFinite(gramsNumber) && gramsNumber > 0) {
      if (option.grams === 1 && option.key === 'g') {
        setAmount(String(Math.round(gramsNumber)))
      } else {
        const qty = gramsNumber / option.grams
        setAmount(Number.isInteger(qty) ? String(qty) : qty.toFixed(2))
      }
    } else {
      setAmount(option.grams === 1 && option.key === 'g' ? '100' : '1')
    }
    setServingKey(option.key)
  }

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

        <div className="field">
          <span>{t.addFood.portionUnit}</span>
          <div className="portion-presets" role="listbox" aria-label={t.addFood.portionUnit}>
            {servings.map((option) => (
              <button
                key={option.key}
                type="button"
                role="option"
                aria-selected={option.key === activeServing.key}
                className={
                  option.key === activeServing.key
                    ? 'portion-preset is-active'
                    : 'portion-preset'
                }
                onClick={() => selectServing(option)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        <label className="field">
          <span>
            {isGramsUnit
              ? t.addFood.portionGrams
              : t.addFood.portionAmount(activeServing.label)}
          </span>
          <input
            inputMode="decimal"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            autoFocus
          />
        </label>

        {!isGramsUnit && Number.isFinite(gramsNumber) && gramsNumber > 0 ? (
          <p className="muted small">
            {t.addFood.portionEqualsGrams(round0(gramsNumber))}
          </p>
        ) : null}

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
