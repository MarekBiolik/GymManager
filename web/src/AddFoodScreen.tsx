import { useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  createEntry,
  createProduct,
  fetchMyProducts,
  fetchRecentProducts,
} from './api'
import { macrosForGrams, round0, round1 } from './nutrition'
import type { MealId, Product } from './types'
import { MEAL_LABELS, MEAL_ORDER } from './types'

type Tab = 'recent' | 'mine' | 'custom'

type Props = {
  date: string
  initialMeal: MealId
  onClose: () => void
  onSaved: () => void
}

export function AddFoodScreen({ date, initialMeal, onClose, onSaved }: Props) {
  const [tab, setTab] = useState<Tab>('recent')
  const [meal, setMeal] = useState<MealId>(initialMeal)
  const [recent, setRecent] = useState<Product[]>([])
  const [mine, setMine] = useState<Product[]>([])
  const [selected, setSelected] = useState<Product | null>(null)
  const [grams, setGrams] = useState('100')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [name, setName] = useState('')
  const [brand, setBrand] = useState('')
  const [kcal, setKcal] = useState('')
  const [protein, setProtein] = useState('')
  const [carbs, setCarbs] = useState('')
  const [fat, setFat] = useState('')

  useEffect(() => {
    let cancelled = false
    Promise.all([fetchRecentProducts(), fetchMyProducts()])
      .then(([recentProducts, myProducts]) => {
        if (cancelled) return
        setRecent(recentProducts)
        setMine(myProducts)
      })
      .catch((err: Error) => {
        if (!cancelled) setError(err.message)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const gramsNumber = Number(grams)
  const preview = useMemo(() => {
    if (!selected || !Number.isFinite(gramsNumber) || gramsNumber <= 0) return null
    return macrosForGrams(selected, gramsNumber)
  }, [selected, gramsNumber])

  async function saveSelected() {
    if (!selected || !Number.isFinite(gramsNumber) || gramsNumber <= 0) {
      setError('Podaj poprawną gramaturę')
      return
    }
    setSaving(true)
    setError(null)
    try {
      await createEntry({
        entry_date: date,
        meal,
        product_id: selected.id,
        grams: gramsNumber,
      })
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Nie udało się zapisać')
    } finally {
      setSaving(false)
    }
  }

  async function saveCustom(event: FormEvent) {
    event.preventDefault()
    const kcalN = Number(kcal)
    const proteinN = Number(protein)
    const carbsN = Number(carbs)
    const fatN = Number(fat)

    if (!name.trim()) {
      setError('Podaj nazwę produktu')
      return
    }
    if (
      ![kcalN, proteinN, carbsN, fatN, gramsNumber].every(
        (n) => Number.isFinite(n) && n >= 0,
      ) ||
      gramsNumber <= 0
    ) {
      setError('Uzupełnij makro na 100 g i gramaturę')
      return
    }

    setSaving(true)
    setError(null)
    try {
      const product = await createProduct({
        name: name.trim(),
        brand: brand.trim() || undefined,
        kcal_100g: kcalN,
        protein_100g: proteinN,
        carbs_100g: carbsN,
        fat_100g: fatN,
      })
      await createEntry({
        entry_date: date,
        meal,
        product_id: product.id,
        grams: gramsNumber,
      })
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Nie udało się zapisać')
    } finally {
      setSaving(false)
    }
  }

  function renderProductList(products: Product[], emptyText: string) {
    if (products.length === 0) {
      return <p className="empty-hint">{emptyText}</p>
    }
    return (
      <ul className="product-list">
        {products.map((product) => (
          <li key={product.id}>
            <button
              type="button"
              className={
                selected?.id === product.id
                  ? 'product-row is-selected'
                  : 'product-row'
              }
              onClick={() => {
                setSelected(product)
                setError(null)
              }}
            >
              <div>
                <strong>{product.name}</strong>
                {product.brand ? <span className="muted"> · {product.brand}</span> : null}
                <div className="muted small">
                  {round0(product.kcal_100g)} kcal · B {round1(product.protein_100g)} · W{' '}
                  {round1(product.carbs_100g)} · T {round1(product.fat_100g)} / 100 g
                </div>
              </div>
            </button>
          </li>
        ))}
      </ul>
    )
  }

  return (
    <div className="overlay-screen">
      <header className="overlay-header">
        <button type="button" className="text-btn" onClick={onClose}>
          Anuluj
        </button>
        <h1>Dodaj jedzenie</h1>
        <span className="header-spacer" />
      </header>

      <div className="overlay-body">
        <label className="field">
          <span>Posiłek</span>
          <select value={meal} onChange={(e) => setMeal(e.target.value as MealId)}>
            {MEAL_ORDER.map((id) => (
              <option key={id} value={id}>
                {MEAL_LABELS[id]}
              </option>
            ))}
          </select>
        </label>

        <div className="tabs" role="tablist">
          <button
            type="button"
            className={tab === 'recent' ? 'tab is-active' : 'tab'}
            onClick={() => setTab('recent')}
          >
            Ostatnie
          </button>
          <button
            type="button"
            className={tab === 'mine' ? 'tab is-active' : 'tab'}
            onClick={() => setTab('mine')}
          >
            Moje
          </button>
          <button
            type="button"
            className={tab === 'custom' ? 'tab is-active' : 'tab'}
            onClick={() => {
              setTab('custom')
              setSelected(null)
            }}
          >
            Własny
          </button>
        </div>

        {tab === 'recent' &&
          renderProductList(recent, 'Tu pojawią się ostatnio dodane produkty.')}
        {tab === 'mine' &&
          renderProductList(mine, 'Nie masz jeszcze własnych produktów.')}

        {tab === 'custom' && (
          <form className="custom-form" onSubmit={saveCustom}>
            <label className="field">
              <span>Nazwa</span>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="np. Pierś z kurczaka"
                required
              />
            </label>
            <label className="field">
              <span>Marka (opcjonalnie)</span>
              <input
                value={brand}
                onChange={(e) => setBrand(e.target.value)}
                placeholder="np. Biedronka"
              />
            </label>
            <div className="macro-grid">
              <label className="field">
                <span>kcal / 100 g</span>
                <input
                  inputMode="decimal"
                  value={kcal}
                  onChange={(e) => setKcal(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Białko</span>
                <input
                  inputMode="decimal"
                  value={protein}
                  onChange={(e) => setProtein(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Węgle</span>
                <input
                  inputMode="decimal"
                  value={carbs}
                  onChange={(e) => setCarbs(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>Tłuszcz</span>
                <input
                  inputMode="decimal"
                  value={fat}
                  onChange={(e) => setFat(e.target.value)}
                  required
                />
              </label>
            </div>
            <label className="field">
              <span>Porcja (g)</span>
              <input
                inputMode="decimal"
                value={grams}
                onChange={(e) => setGrams(e.target.value)}
                required
              />
            </label>
            {error ? <p className="error">{error}</p> : null}
            <button type="submit" className="primary-btn" disabled={saving}>
              {saving ? 'Zapisuję…' : 'Dodaj do dziennika'}
            </button>
          </form>
        )}

        {tab !== 'custom' && selected ? (
          <div className="portion-card">
            <div className="portion-title">
              {selected.name}
              {selected.brand ? ` · ${selected.brand}` : ''}
            </div>
            <label className="field">
              <span>Porcja (g)</span>
              <input
                inputMode="decimal"
                value={grams}
                onChange={(e) => setGrams(e.target.value)}
              />
            </label>
            {preview ? (
              <p className="muted">
                {round0(preview.kcal)} kcal · B {round1(preview.protein)} · W{' '}
                {round1(preview.carbs)} · T {round1(preview.fat)}
              </p>
            ) : null}
            {error ? <p className="error">{error}</p> : null}
            <button
              type="button"
              className="primary-btn"
              disabled={saving}
              onClick={saveSelected}
            >
              {saving ? 'Zapisuję…' : 'Dodaj do dziennika'}
            </button>
          </div>
        ) : null}

        {tab !== 'custom' && !selected && error ? (
          <p className="error">{error}</p>
        ) : null}
      </div>
    </div>
  )
}
