import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from 'react'
import {
  createEntry,
  createProduct,
  fetchFrequentProducts,
  fetchMyProducts,
  lookupBarcode,
  searchFoods,
  type FoodCandidate,
} from './api'
import { BarcodeScannerOverlay } from './BarcodeScannerOverlay'
import { canUseCameraBarcodeScan, extractBarcode } from './barcodeScan'
import { t } from './i18n'
import { round0, round1 } from './nutrition'
import { PortionScreen } from './PortionScreen'
import type { MealId, Product } from './types'

type Tab = 'search' | 'mine' | 'custom'

type Selectable = Product | FoodCandidate

function isSavedProduct(item: Selectable): item is Product {
  return 'id' in item && typeof item.id === 'number'
}

type Props = {
  date: string
  meal: MealId
  onClose: () => void
  onSaved: () => void
}

export function AddFoodScreen({ date, meal, onClose, onSaved }: Props) {
  const [tab, setTab] = useState<Tab>('search')
  const [frequent, setFrequent] = useState<Product[]>([])
  const [mine, setMine] = useState<Product[]>([])
  const [pending, setPending] = useState<Selectable | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [savingCustom, setSavingCustom] = useState(false)

  const [query, setQuery] = useState('')
  const [customBarcode, setCustomBarcode] = useState('')
  const [searching, setSearching] = useState(false)
  const [localHits, setLocalHits] = useState<Product[]>([])
  const [remoteHits, setRemoteHits] = useState<FoodCandidate[]>([])
  const [scanning, setScanning] = useState(false)
  const searchInputRef = useRef<HTMLInputElement>(null)
  const searchRequestId = useRef(0)

  const [name, setName] = useState('')
  const [brand, setBrand] = useState('')
  const [kcal, setKcal] = useState('')
  const [protein, setProtein] = useState('')
  const [carbs, setCarbs] = useState('')
  const [fat, setFat] = useState('')

  const mealLabel = t.meals[meal]

  const reloadLists = useCallback(async () => {
    const [freq, my] = await Promise.all([
      fetchFrequentProducts(meal),
      fetchMyProducts(),
    ])
    setFrequent(freq)
    setMine(my)
  }, [meal])

  useEffect(() => {
    let cancelled = false
    reloadLists().catch((err: Error) => {
      if (!cancelled) setError(err.message)
    })
    return () => {
      cancelled = true
    }
  }, [reloadLists])

  function openCustomBlank(prefillBarcode?: string) {
    setTab('custom')
    setPending(null)
    setName('')
    setBrand('')
    setKcal('')
    setProtein('')
    setCarbs('')
    setFat('')
    setCustomBarcode(prefillBarcode ?? '')
  }

  function clearSearch() {
    searchRequestId.current += 1
    setQuery('')
    setLocalHits([])
    setRemoteHits([])
    setError(null)
    setSearching(false)
    searchInputRef.current?.focus()
  }

  const resolveBarcode = useCallback(async (code: string) => {
    const requestId = ++searchRequestId.current
    setSearching(true)
    setError(null)
    setLocalHits([])
    setRemoteHits([])
    try {
      const result = await lookupBarcode(code)
      if (requestId !== searchRequestId.current) return
      if (!result.found) {
        openCustomBlank(code)
        setError(t.addFood.barcodeNotFound)
        return
      }
      setTab('search')
      setQuery(code)
      setPending(result.product)
    } catch (err) {
      if (requestId !== searchRequestId.current) return
      setError(err instanceof Error ? err.message : t.addFood.barcodeFailed)
    } finally {
      if (requestId === searchRequestId.current) setSearching(false)
    }
  }, [])

  const runTextSearch = useCallback(async (q: string) => {
    const requestId = ++searchRequestId.current
    setSearching(true)
    setError(null)
    try {
      const result = await searchFoods(q)
      if (requestId !== searchRequestId.current) return
      setLocalHits(result.local)
      setRemoteHits(result.remote)
      if (result.warning) {
        setError(result.warning)
      } else if (result.local.length === 0 && result.remote.length === 0) {
        setError(t.addFood.noResults)
      }
    } catch (err) {
      if (requestId !== searchRequestId.current) return
      setError(err instanceof Error ? err.message : t.addFood.searchFailed)
    } finally {
      if (requestId === searchRequestId.current) setSearching(false)
    }
  }, [])

  useEffect(() => {
    const q = query.trim()
    if (q.length === 0) {
      searchRequestId.current += 1
      setLocalHits([])
      setRemoteHits([])
      setSearching(false)
      setError(null)
      return
    }

    const asBarcode = extractBarcode(q)
    if (
      asBarcode &&
      asBarcode === q.replace(/\D/g, '') &&
      q.replace(/\D/g, '').length >= 8
    ) {
      const handle = window.setTimeout(() => {
        void resolveBarcode(asBarcode)
      }, 350)
      return () => window.clearTimeout(handle)
    }

    if (q.length < 2) {
      searchRequestId.current += 1
      setLocalHits([])
      setRemoteHits([])
      setSearching(false)
      setError(null)
      return
    }

    const handle = window.setTimeout(() => {
      void runTextSearch(q)
    }, 300)
    return () => window.clearTimeout(handle)
  }, [query, resolveBarcode, runTextSearch])

  function openScanner() {
    if (canUseCameraBarcodeScan()) {
      setError(null)
      setScanning(true)
      return
    }
    setError(t.addFood.scanFallbackHint)
    searchInputRef.current?.focus()
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

  async function confirmPortion(grams: number) {
    if (!pending) return
    const product = await ensureProduct(pending)
    await createEntry({
      entry_date: date,
      meal,
      product_id: product.id,
      grams,
    })
    onSaved()
  }

  async function saveCustom(event: FormEvent) {
    event.preventDefault()
    const kcalN = Number(kcal)
    const proteinN = Number(protein)
    const carbsN = Number(carbs)
    const fatN = Number(fat)
    const code = customBarcode.replace(/\D/g, '')

    if (!name.trim()) {
      setError(t.addFood.nameRequired)
      return
    }
    if (
      ![kcalN, proteinN, carbsN, fatN].every((n) => Number.isFinite(n) && n >= 0)
    ) {
      setError(t.addFood.macrosRequired)
      return
    }

    setSavingCustom(true)
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
      setPending(product)
      setTab('search')
    } catch (err) {
      setError(err instanceof Error ? err.message : t.addFood.saveFailed)
    } finally {
      setSavingCustom(false)
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
          return (
            <li key={key}>
              <button
                type="button"
                className="product-row"
                onClick={() => {
                  setPending(product)
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

  if (pending) {
    return (
      <PortionScreen
        product={pending}
        confirmLabel={t.addFood.addToDiary}
        onBack={() => setPending(null)}
        onConfirm={confirmPortion}
      />
    )
  }

  const showFrequent = tab === 'search' && query.trim().length === 0

  return (
    <div className="overlay-screen">
      <header className="overlay-header">
        <button type="button" className="text-btn" onClick={onClose}>
          {t.addFood.cancel}
        </button>
        <h1>{t.addFood.titleForMeal(mealLabel)}</h1>
        <span className="header-spacer" />
      </header>

      <div className="overlay-body">
        <div className="tabs tabs-3" role="tablist">
          <button
            type="button"
            className={tab === 'search' ? 'tab is-active' : 'tab'}
            onClick={() => setTab('search')}
          >
            {t.addFood.tabSearch}
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
            <div className="search-bar">
              <button
                type="button"
                className="search-icon-btn"
                aria-label={t.addFood.scanAria}
                title={t.addFood.scanAria}
                onClick={openScanner}
              >
                <BarcodeIcon />
              </button>
              <div className="search-input-wrap">
                <input
                  ref={searchInputRef}
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder={t.addFood.searchPlaceholder}
                  enterKeyHint="search"
                  autoComplete="off"
                  autoCorrect="off"
                  spellCheck={false}
                />
                {query ? (
                  <button
                    type="button"
                    className="search-clear-btn"
                    aria-label={t.addFood.clearSearch}
                    onClick={clearSearch}
                  >
                    ×
                  </button>
                ) : null}
              </div>
            </div>
            {searching ? (
              <p className="muted small">{t.addFood.searching}</p>
            ) : null}

            {showFrequent ? (
              <>
                <h2 className="section-title">{t.addFood.frequent}</h2>
                {renderProductList(frequent, t.addFood.frequentEmpty)}
              </>
            ) : null}

            {!showFrequent && localHits.length > 0 ? (
              <>
                <h2 className="section-title">{t.addFood.inApp}</h2>
                {renderProductList(localHits, '')}
              </>
            ) : null}
            {!showFrequent && remoteHits.length > 0 ? (
              <>
                <h2 className="section-title">{t.addFood.openFoodFacts}</h2>
                {renderProductList(remoteHits, '')}
              </>
            ) : null}
          </div>
        )}

        {tab === 'mine' && renderProductList(mine, t.addFood.mineEmpty)}

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
                value={customBarcode}
                onChange={(e) => setCustomBarcode(e.target.value)}
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
            {error ? <p className="error">{error}</p> : null}
            <button type="submit" className="primary-btn" disabled={savingCustom}>
              {savingCustom ? t.addFood.saving : t.addFood.continueToPortion}
            </button>
          </form>
        )}

        {tab !== 'custom' && error ? <p className="error">{error}</p> : null}
      </div>

      {scanning ? (
        <BarcodeScannerOverlay
          onDetected={(code) => {
            setScanning(false)
            setQuery(code)
            void resolveBarcode(code)
          }}
          onCancel={() => setScanning(false)}
          onError={(message) => setError(message)}
        />
      ) : null}
    </div>
  )
}

function BarcodeIcon() {
  return (
    <svg
      width="22"
      height="22"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
      />
      <path
        d="M7 7h.01M10 7v10M12.5 7v10M15 7v10M17.5 7v10"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinecap="round"
      />
    </svg>
  )
}
