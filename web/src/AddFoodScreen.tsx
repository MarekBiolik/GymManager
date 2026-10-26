import { useEffect, useMemo, useState, type FormEvent } from 'react'
import {
  createEntry,
  createProduct,
  fetchMyProducts,
  fetchRecentProducts,
  lookupBarcode,
  searchFoods,
  type FoodCandidate,
} from './api'
import { t } from './i18n'
import { macrosForGrams, round0, round1 } from './nutrition'
import type { MealId, Product } from './types'
import { MEAL_ORDER } from './types'

type Tab = 'search' | 'recent' | 'mine' | 'custom'

type Selectable = Product | FoodCandidate

function isSavedProduct(item: Selectable): item is Product {
  return 'id' in item && typeof item.id === 'number'
}

type Props = {
  date: string
  initialMeal: MealId
  onClose: () => void
  onSaved: () => void
}

export function AddFoodScreen({ date, initialMeal, onClose, onSaved }: Props) {
  const [tab, setTab] = useState<Tab>('search')
  const [meal, setMeal] = useState<MealId>(initialMeal)
  const [recent, setRecent] = useState<Product[]>([])
  const [mine, setMine] = useState<Product[]>([])
  const [selected, setSelected] = useState<Selectable | null>(null)
  const [grams, setGrams] = useState('100')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const [query, setQuery] = useState('')
  const [barcode, setBarcode] = useState('')
  const [searching, setSearching] = useState(false)
  const [localHits, setLocalHits] = useState<Product[]>([])
  const [remoteHits, setRemoteHits] = useState<FoodCandidate[]>([])

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

  function openCustomBlank(prefillBarcode?: string) {
    setTab('custom')
    setSelected(null)
    setName('')
    setBrand('')
    setKcal('')
    setProtein('')
    setCarbs('')
    setFat('')
    if (prefillBarcode) setBarcode(prefillBarcode)
  }

  async function runSearch(event?: FormEvent) {
    event?.preventDefault()
    const q = query.trim()
    if (q.length < 2) {
      setError(t.addFood.queryTooShort)
      return
    }
    setSearching(true)
    setError(null)
    setSelected(null)
    try {
      const result = await searchFoods(q)
      setLocalHits(result.local)
      setRemoteHits(result.remote)
      if (result.warning) {
        setError(result.warning)
      } else if (result.local.length === 0 && result.remote.length === 0) {
        setError(t.addFood.noResults)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t.addFood.searchFailed)
    } finally {
      setSearching(false)
    }
  }

  async function runBarcode(event?: FormEvent) {
    event?.preventDefault()
    const code = barcode.replace(/\D/g, '')
    if (code.length < 8) {
      setError(t.addFood.barcodeTooShort)
      return
    }
    setSearching(true)
    setError(null)
    setSelected(null)
    try {
      const result = await lookupBarcode(code)
      if (!result.found) {
        openCustomBlank(code)
        setError(t.addFood.barcodeNotFound)
        return
      }
      setTab('search')
      setLocalHits(result.origin === 'local' ? [result.product] : [])
      setRemoteHits(result.origin === 'openfoodfacts' ? [result.product] : [])
      setSelected(result.product)
    } catch (err) {
      setError(err instanceof Error ? err.message : t.addFood.barcodeFailed)
    } finally {
      setSearching(false)
    }
  }

  async function ensureProduct(item: Selectable): Promise<Product> {
    if (isSavedProduct(item)) return item
    return createProduct({
      name: item.name,
      brand: item.brand ?? undefined,
      barcode: item.barcode ?? undefined,
      kcal_100g: item.kcal_100g,
      protein_100g: item.protein_100g,
      carbs_100g: item.carbs_100g,
      fat_100g: item.fat_100g,
      source: 'openfoodfacts',
    })
  }

  async function saveSelected() {
    if (!selected || !Number.isFinite(gramsNumber) || gramsNumber <= 0) {
      setError(t.addFood.invalidGrams)
      return
    }
    setSaving(true)
    setError(null)
    try {
      const product = await ensureProduct(selected)
      await createEntry({
        entry_date: date,
        meal,
        product_id: product.id,
        grams: gramsNumber,
      })
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : t.addFood.saveFailed)
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
    const code = barcode.replace(/\D/g, '')

    if (!name.trim()) {
      setError(t.addFood.nameRequired)
      return
    }
    if (
      ![kcalN, proteinN, carbsN, fatN, gramsNumber].every(
        (n) => Number.isFinite(n) && n >= 0,
      ) ||
      gramsNumber <= 0
    ) {
      setError(t.addFood.macrosRequired)
      return
    }

    setSaving(true)
    setError(null)
    try {
      const product = await createProduct({
        name: name.trim(),
        brand: brand.trim() || undefined,
        barcode: code || undefined,
        kcal_100g: kcalN,
        protein_100g: proteinN,
        carbs_100g: carbsN,
        fat_100g: fatN,
        source: 'custom',
      })
      await createEntry({
        entry_date: date,
        meal,
        product_id: product.id,
        grams: gramsNumber,
      })
      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : t.addFood.saveFailed)
    } finally {
      setSaving(false)
    }
  }

  function renderProductList(products: Selectable[], emptyText: string) {
    if (products.length === 0) {
      return <p className="empty-hint">{emptyText}</p>
    }
    return (
      <ul className="product-list">
        {products.map((product) => {
          const key = isSavedProduct(product)
            ? `local-${product.id}`
            : `off-${product.barcode ?? product.name}`
          const selectedKey = selected
            ? isSavedProduct(selected)
              ? `local-${selected.id}`
              : `off-${selected.barcode ?? selected.name}`
            : null
          return (
            <li key={key}>
              <button
                type="button"
                className={
                  selectedKey === key ? 'product-row is-selected' : 'product-row'
                }
                onClick={() => {
                  setSelected(product)
                  setError(null)
                }}
              >
                <div>
                  <strong>{product.name}</strong>
                  {product.brand ? (
                    <span className="muted"> · {product.brand}</span>
                  ) : null}
                  <div className="muted small">
                    {round0(product.kcal_100g)} kcal · B{' '}
                    {round1(product.protein_100g)} · W {round1(product.carbs_100g)} · T{' '}
                    {round1(product.fat_100g)} / 100 g
                    {!isSavedProduct(product) ? ' · OFF' : ''}
                  </div>
                </div>
              </button>
            </li>
          )
        })}
      </ul>
    )
  }

  return (
    <div className="overlay-screen">
      <header className="overlay-header">
        <button type="button" className="text-btn" onClick={onClose}>
          {t.addFood.cancel}
        </button>
        <h1>{t.addFood.title}</h1>
        <span className="header-spacer" />
      </header>

      <div className="overlay-body">
        <label className="field">
          <span>{t.addFood.meal}</span>
          <select value={meal} onChange={(e) => setMeal(e.target.value as MealId)}>
            {MEAL_ORDER.map((id) => (
              <option key={id} value={id}>
                {t.meals[id]}
              </option>
            ))}
          </select>
        </label>

        <div className="tabs tabs-4" role="tablist">
          <button
            type="button"
            className={tab === 'search' ? 'tab is-active' : 'tab'}
            onClick={() => setTab('search')}
          >
            {t.addFood.tabSearch}
          </button>
          <button
            type="button"
            className={tab === 'recent' ? 'tab is-active' : 'tab'}
            onClick={() => setTab('recent')}
          >
            {t.addFood.tabRecent}
          </button>
          <button
            type="button"
            className={tab === 'mine' ? 'tab is-active' : 'tab'}
            onClick={() => setTab('mine')}
          >
            {t.addFood.tabMine}
          </button>
          <button
            type="button"
            className={tab === 'custom' ? 'tab is-active' : 'tab'}
            onClick={() => openCustomBlank()}
          >
            {t.addFood.tabCustom}
          </button>
        </div>

        {tab === 'search' && (
          <div className="search-panel">
            <form className="search-row" onSubmit={runSearch}>
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={t.addFood.searchPlaceholder}
                enterKeyHint="search"
              />
              <button type="submit" className="primary-btn compact" disabled={searching}>
                {searching ? '…' : t.addFood.search}
              </button>
            </form>
            <form className="search-row" onSubmit={runBarcode}>
              <input
                value={barcode}
                onChange={(e) => setBarcode(e.target.value)}
                placeholder={t.addFood.barcodePlaceholder}
                inputMode="numeric"
              />
              <button type="submit" className="primary-btn compact" disabled={searching}>
                {t.addFood.barcode}
              </button>
            </form>
            {localHits.length > 0 ? (
              <>
                <h2 className="section-title">{t.addFood.inApp}</h2>
                {renderProductList(localHits, '')}
              </>
            ) : null}
            {remoteHits.length > 0 ? (
              <>
                <h2 className="section-title">{t.addFood.openFoodFacts}</h2>
                {renderProductList(remoteHits, '')}
              </>
            ) : null}
          </div>
        )}

        {tab === 'recent' &&
          renderProductList(recent, t.addFood.recentEmpty)}
        {tab === 'mine' &&
          renderProductList(mine, t.addFood.mineEmpty)}

        {tab === 'custom' && (
          <form className="custom-form" onSubmit={saveCustom}>
            <label className="field">
              <span>{t.addFood.name}</span>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t.addFood.namePlaceholder}
                required
              />
            </label>
            <label className="field">
              <span>{t.addFood.brandOptional}</span>
              <input
                value={brand}
                onChange={(e) => setBrand(e.target.value)}
                placeholder={t.addFood.brandPlaceholder}
              />
            </label>
            <label className="field">
              <span>{t.addFood.barcodeOptional}</span>
              <input
                value={barcode}
                onChange={(e) => setBarcode(e.target.value)}
                inputMode="numeric"
              />
            </label>
            <div className="macro-grid">
              <label className="field">
                <span>{t.addFood.kcalPer100}</span>
                <input
                  inputMode="decimal"
                  value={kcal}
                  onChange={(e) => setKcal(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>{t.addFood.protein}</span>
                <input
                  inputMode="decimal"
                  value={protein}
                  onChange={(e) => setProtein(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>{t.addFood.carbs}</span>
                <input
                  inputMode="decimal"
                  value={carbs}
                  onChange={(e) => setCarbs(e.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>{t.addFood.fat}</span>
                <input
                  inputMode="decimal"
                  value={fat}
                  onChange={(e) => setFat(e.target.value)}
                  required
                />
              </label>
            </div>
            <label className="field">
              <span>{t.addFood.portionGrams}</span>
              <input
                inputMode="decimal"
                value={grams}
                onChange={(e) => setGrams(e.target.value)}
                required
              />
            </label>
            {error ? <p className="error">{error}</p> : null}
            <button type="submit" className="primary-btn" disabled={saving}>
              {saving ? t.addFood.saving : t.addFood.addToDiary}
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
              <span>{t.addFood.portionGrams}</span>
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
              onClick={() => void saveSelected()}
            >
              {saving ? t.addFood.saving : t.addFood.addToDiary}
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
