import cors from 'cors'
import express from 'express'
import {
  addDiaryEntry,
  createProduct,
  deleteDiaryEntry,
  findByBarcode,
  getDay,
  getGoals,
  listCustomProducts,
  listRecentProducts,
  searchLocalProducts,
  setGoals,
  type MealId,
} from './db.ts'
import { fetchByBarcode, searchOff } from './openFoodFacts.ts'

const PORT = Number(process.env.PORT ?? 3001)
const MEALS = new Set<MealId>(['breakfast', 'lunch', 'dinner', 'snacks'])

const app = express()
app.use(cors())
app.use(express.json())

function todayLocal(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function asNumber(value: unknown): number | null {
  const n = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(n) ? n : null
}

app.get('/api/health', (_req, res) => {
  res.json({ ok: true })
})

app.get('/api/goals', (_req, res) => {
  res.json(getGoals())
})

app.put('/api/goals', (req, res) => {
  const kcal = asNumber(req.body?.kcal)
  const protein = asNumber(req.body?.protein)
  const carbs = asNumber(req.body?.carbs)
  const fat = asNumber(req.body?.fat)

  if (
    kcal == null ||
    protein == null ||
    carbs == null ||
    fat == null ||
    kcal <= 0 ||
    protein < 0 ||
    carbs < 0 ||
    fat < 0
  ) {
    res.status(400).json({ error: 'Invalid goals' })
    return
  }

  res.json(setGoals({ kcal, protein, carbs, fat }))
})

app.get('/api/day', (req, res) => {
  const date =
    typeof req.query.date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(req.query.date)
      ? req.query.date
      : todayLocal()
  res.json(getDay(date))
})

app.get('/api/products/recent', (_req, res) => {
  res.json(listRecentProducts())
})

app.get('/api/products/mine', (_req, res) => {
  res.json(listCustomProducts())
})

app.get('/api/foods/search', async (req, res) => {
  const q = typeof req.query.q === 'string' ? req.query.q.trim() : ''
  if (q.length < 2) {
    res.json({ local: [], remote: [] })
    return
  }

  try {
    const local = searchLocalProducts(q)
    const remote = await searchOff(q)
    const localKeys = new Set(
      local
        .map((p) => p.barcode)
        .filter((code): code is string => Boolean(code)),
    )
    const remoteFiltered = remote.filter(
      (item) => !item.barcode || !localKeys.has(item.barcode),
    )
    res.json({ local, remote: remoteFiltered })
  } catch (err) {
    res.status(502).json({
      error: err instanceof Error ? err.message : 'Open Food Facts unavailable',
    })
  }
})

app.get('/api/foods/barcode/:code', async (req, res) => {
  const code = String(req.params.code ?? '').replace(/\D/g, '')
  if (code.length < 8) {
    res.status(400).json({ error: 'Invalid barcode' })
    return
  }

  const local = findByBarcode(code)
  if (local) {
    res.json({ found: true, origin: 'local', product: local })
    return
  }

  try {
    const remote = await fetchByBarcode(code)
    if (!remote) {
      res.json({ found: false, barcode: code })
      return
    }
    res.json({ found: true, origin: 'openfoodfacts', product: remote })
  } catch (err) {
    res.status(502).json({
      error: err instanceof Error ? err.message : 'Open Food Facts unavailable',
    })
  }
})

app.post('/api/products', (req, res) => {
  const name = String(req.body?.name ?? '').trim()
  const kcal_100g = asNumber(req.body?.kcal_100g)
  const protein_100g = asNumber(req.body?.protein_100g)
  const carbs_100g = asNumber(req.body?.carbs_100g)
  const fat_100g = asNumber(req.body?.fat_100g)
  const source =
    req.body?.source === 'openfoodfacts' ? 'openfoodfacts' : 'custom'

  if (
    !name ||
    kcal_100g == null ||
    protein_100g == null ||
    carbs_100g == null ||
    fat_100g == null ||
    kcal_100g < 0 ||
    protein_100g < 0 ||
    carbs_100g < 0 ||
    fat_100g < 0
  ) {
    res.status(400).json({ error: 'Invalid product' })
    return
  }

  const barcode =
    typeof req.body?.barcode === 'string' ? req.body.barcode.trim() : null
  if (barcode) {
    const existing = findByBarcode(barcode)
    if (existing) {
      res.status(201).json(existing)
      return
    }
  }

  const product = createProduct({
    name,
    brand: req.body?.brand ?? null,
    barcode,
    kcal_100g,
    protein_100g,
    carbs_100g,
    fat_100g,
    source,
  })
  res.status(201).json(product)
})

app.post('/api/entries', (req, res) => {
  const entry_date =
    typeof req.body?.entry_date === 'string' &&
    /^\d{4}-\d{2}-\d{2}$/.test(req.body.entry_date)
      ? req.body.entry_date
      : todayLocal()
  const meal = req.body?.meal as MealId
  const product_id = asNumber(req.body?.product_id)
  const grams = asNumber(req.body?.grams)

  if (!MEALS.has(meal) || product_id == null || grams == null || grams <= 0) {
    res.status(400).json({ error: 'Invalid entry' })
    return
  }

  try {
    const entry = addDiaryEntry({
      entry_date,
      meal,
      product_id,
      grams,
    })
    res.status(201).json(entry)
  } catch {
    res.status(404).json({ error: 'Product not found' })
  }
})

app.delete('/api/entries/:id', (req, res) => {
  const id = Number(req.params.id)
  if (!Number.isInteger(id)) {
    res.status(400).json({ error: 'Invalid id' })
    return
  }
  const deleted = deleteDiaryEntry(id)
  if (!deleted) {
    res.status(404).json({ error: 'Entry not found' })
    return
  }
  res.status(204).end()
})

app.listen(PORT, '0.0.0.0', () => {
  console.log(`GymManager API on http://0.0.0.0:${PORT}`)
})
