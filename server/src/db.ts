import Database from 'better-sqlite3'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const dataDir = path.resolve(__dirname, '../../data')
const dbPath = path.join(dataDir, 'gymmanager.db')

fs.mkdirSync(dataDir, { recursive: true })

export const db = new Database(dbPath)

db.pragma('journal_mode = WAL')
db.pragma('foreign_keys = ON')

db.exec(`
  CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    brand TEXT,
    barcode TEXT,
    kcal_100g REAL NOT NULL,
    protein_100g REAL NOT NULL,
    carbs_100g REAL NOT NULL,
    fat_100g REAL NOT NULL,
    source TEXT NOT NULL CHECK (source IN ('custom', 'openfoodfacts')),
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS diary_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entry_date TEXT NOT NULL,
    meal TEXT NOT NULL CHECK (meal IN ('breakfast', 'lunch', 'dinner', 'snacks')),
    product_id INTEGER NOT NULL REFERENCES products(id),
    grams REAL NOT NULL CHECK (grams > 0),
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
  );

  CREATE INDEX IF NOT EXISTS idx_diary_date ON diary_entries(entry_date);
  CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode);
`)

const DEFAULT_GOALS = {
  kcal: 2200,
  protein: 160,
  carbs: 220,
  fat: 70,
}

for (const [key, value] of Object.entries(DEFAULT_GOALS)) {
  db.prepare(
    `INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)`,
  ).run(`goal_${key}`, String(value))
}

export type MealId = 'breakfast' | 'lunch' | 'dinner' | 'snacks'

export type Product = {
  id: number
  name: string
  brand: string | null
  barcode: string | null
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  source: 'custom' | 'openfoodfacts'
  created_at: string
}

export type Goals = {
  kcal: number
  protein: number
  carbs: number
  fat: number
}

export type DiaryEntry = {
  id: number
  entry_date: string
  meal: MealId
  grams: number
  created_at: string
  product: Product
}

function rowToProduct(row: Record<string, unknown>): Product {
  return {
    id: Number(row.id),
    name: String(row.name),
    brand: row.brand == null ? null : String(row.brand),
    barcode: row.barcode == null ? null : String(row.barcode),
    kcal_100g: Number(row.kcal_100g),
    protein_100g: Number(row.protein_100g),
    carbs_100g: Number(row.carbs_100g),
    fat_100g: Number(row.fat_100g),
    source: row.source as Product['source'],
    created_at: String(row.created_at),
  }
}

export function getGoals(): Goals {
  const rows = db
    .prepare(`SELECT key, value FROM settings WHERE key LIKE 'goal_%'`)
    .all() as Array<{ key: string; value: string }>

  const goals = { ...DEFAULT_GOALS }
  for (const row of rows) {
    const field = row.key.replace('goal_', '') as keyof Goals
    if (field in goals) {
      goals[field] = Number(row.value)
    }
  }
  return goals
}

export function setGoals(goals: Goals): Goals {
  const upsert = db.prepare(
    `INSERT INTO settings (key, value) VALUES (?, ?)
     ON CONFLICT(key) DO UPDATE SET value = excluded.value`,
  )
  const tx = db.transaction(() => {
    upsert.run('goal_kcal', String(goals.kcal))
    upsert.run('goal_protein', String(goals.protein))
    upsert.run('goal_carbs', String(goals.carbs))
    upsert.run('goal_fat', String(goals.fat))
  })
  tx()
  return getGoals()
}

export function createProduct(input: {
  name: string
  brand?: string | null
  barcode?: string | null
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  source?: Product['source']
}): Product {
  const result = db
    .prepare(
      `INSERT INTO products
        (name, brand, barcode, kcal_100g, protein_100g, carbs_100g, fat_100g, source)
       VALUES (@name, @brand, @barcode, @kcal_100g, @protein_100g, @carbs_100g, @fat_100g, @source)`,
    )
    .run({
      name: input.name.trim(),
      brand: input.brand?.trim() || null,
      barcode: input.barcode?.trim() || null,
      kcal_100g: input.kcal_100g,
      protein_100g: input.protein_100g,
      carbs_100g: input.carbs_100g,
      fat_100g: input.fat_100g,
      source: input.source ?? 'custom',
    })

  const row = db
    .prepare(`SELECT * FROM products WHERE id = ?`)
    .get(result.lastInsertRowid) as Record<string, unknown>
  return rowToProduct(row)
}

export function listRecentProducts(limit = 20): Product[] {
  const rows = db
    .prepare(
      `SELECT p.*
       FROM products p
       JOIN diary_entries e ON e.product_id = p.id
       GROUP BY p.id
       ORDER BY MAX(e.created_at) DESC
       LIMIT ?`,
    )
    .all(limit) as Array<Record<string, unknown>>
  return rows.map(rowToProduct)
}

export function listCustomProducts(limit = 50): Product[] {
  const rows = db
    .prepare(
      `SELECT * FROM products
       WHERE source = 'custom'
       ORDER BY created_at DESC
       LIMIT ?`,
    )
    .all(limit) as Array<Record<string, unknown>>
  return rows.map(rowToProduct)
}

export function getProduct(id: number): Product | null {
  const row = db.prepare(`SELECT * FROM products WHERE id = ?`).get(id) as
    | Record<string, unknown>
    | undefined
  return row ? rowToProduct(row) : null
}

export function addDiaryEntry(input: {
  entry_date: string
  meal: MealId
  product_id: number
  grams: number
}): DiaryEntry {
  const product = getProduct(input.product_id)
  if (!product) {
    throw new Error('Product not found')
  }

  const result = db
    .prepare(
      `INSERT INTO diary_entries (entry_date, meal, product_id, grams)
       VALUES (@entry_date, @meal, @product_id, @grams)`,
    )
    .run({
      entry_date: input.entry_date,
      meal: input.meal,
      product_id: input.product_id,
      grams: input.grams,
    })

  return {
    id: Number(result.lastInsertRowid),
    entry_date: input.entry_date,
    meal: input.meal,
    grams: input.grams,
    created_at: new Date().toISOString(),
    product,
  }
}

export function deleteDiaryEntry(id: number): boolean {
  const result = db.prepare(`DELETE FROM diary_entries WHERE id = ?`).run(id)
  return result.changes > 0
}

export function getDay(entryDate: string): {
  date: string
  goals: Goals
  entries: DiaryEntry[]
} {
  const rows = db
    .prepare(
      `SELECT
         e.id AS entry_id,
         e.entry_date,
         e.meal,
         e.grams,
         e.created_at AS entry_created_at,
         p.id AS product_id,
         p.name,
         p.brand,
         p.barcode,
         p.kcal_100g,
         p.protein_100g,
         p.carbs_100g,
         p.fat_100g,
         p.source,
         p.created_at AS product_created_at
       FROM diary_entries e
       JOIN products p ON p.id = e.product_id
       WHERE e.entry_date = ?
       ORDER BY e.id ASC`,
    )
    .all(entryDate) as Array<Record<string, unknown>>

  const entries: DiaryEntry[] = rows.map((row) => ({
    id: Number(row.entry_id),
    entry_date: String(row.entry_date),
    meal: row.meal as MealId,
    grams: Number(row.grams),
    created_at: String(row.entry_created_at),
    product: {
      id: Number(row.product_id),
      name: String(row.name),
      brand: row.brand == null ? null : String(row.brand),
      barcode: row.barcode == null ? null : String(row.barcode),
      kcal_100g: Number(row.kcal_100g),
      protein_100g: Number(row.protein_100g),
      carbs_100g: Number(row.carbs_100g),
      fat_100g: Number(row.fat_100g),
      source: row.source as Product['source'],
      created_at: String(row.product_created_at),
    },
  }))

  return {
    date: entryDate,
    goals: getGoals(),
    entries,
  }
}
