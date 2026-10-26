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

export type MealId =
  | 'breakfast_1'
  | 'breakfast_2'
  | 'lunch'
  | 'snack'
  | 'dinner'

export const MEAL_IDS: MealId[] = [
  'breakfast_1',
  'breakfast_2',
  'lunch',
  'snack',
  'dinner',
]

export type ServingOption = {
  key: string
  label: string
  grams: number
}

export type ProductSource = 'custom' | 'openfoodfacts' | 'staple'

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
    source TEXT NOT NULL,
    servings_json TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS diary_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entry_date TEXT NOT NULL,
    meal TEXT NOT NULL CHECK (meal IN ('breakfast_1', 'breakfast_2', 'lunch', 'snack', 'dinner')),
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
  CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
`)

migrateDiaryMealsIfNeeded()
ensureProductsColumns()
migrateProductsSourceCheckIfNeeded()

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

export type Product = {
  id: number
  name: string
  brand: string | null
  barcode: string | null
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  source: ProductSource
  servings: ServingOption[]
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

function defaultServings(): ServingOption[] {
  return [{ key: 'g', label: 'gramy', grams: 1 }]
}

function parseServings(raw: unknown): ServingOption[] {
  if (typeof raw !== 'string' || !raw.trim()) return defaultServings()
  try {
    const parsed = JSON.parse(raw) as ServingOption[]
    if (!Array.isArray(parsed) || parsed.length === 0) return defaultServings()
    return parsed.filter(
      (s) =>
        s &&
        typeof s.key === 'string' &&
        typeof s.label === 'string' &&
        typeof s.grams === 'number' &&
        s.grams > 0,
    )
  } catch {
    return defaultServings()
  }
}

function migrateDiaryMealsIfNeeded() {
  const row = db
    .prepare(
      `SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'diary_entries'`,
    )
    .get() as { sql: string } | undefined

  if (!row?.sql || row.sql.includes('breakfast_1')) return

  db.exec(`
    CREATE TABLE diary_entries_v2 (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      entry_date TEXT NOT NULL,
      meal TEXT NOT NULL CHECK (meal IN ('breakfast_1', 'breakfast_2', 'lunch', 'snack', 'dinner')),
      product_id INTEGER NOT NULL REFERENCES products(id),
      grams REAL NOT NULL CHECK (grams > 0),
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );

    INSERT INTO diary_entries_v2 (id, entry_date, meal, product_id, grams, created_at)
    SELECT
      id,
      entry_date,
      CASE meal
        WHEN 'breakfast' THEN 'breakfast_1'
        WHEN 'snacks' THEN 'snack'
        WHEN 'breakfast_1' THEN 'breakfast_1'
        WHEN 'breakfast_2' THEN 'breakfast_2'
        WHEN 'lunch' THEN 'lunch'
        WHEN 'dinner' THEN 'dinner'
        WHEN 'snack' THEN 'snack'
        ELSE 'snack'
      END,
      product_id,
      grams,
      created_at
    FROM diary_entries;

    DROP TABLE diary_entries;
    ALTER TABLE diary_entries_v2 RENAME TO diary_entries;
    CREATE INDEX IF NOT EXISTS idx_diary_date ON diary_entries(entry_date);
  `)
}

function ensureProductsColumns() {
  const cols = db.prepare(`PRAGMA table_info(products)`).all() as Array<{
    name: string
  }>
  const names = new Set(cols.map((c) => c.name))
  if (!names.has('servings_json')) {
    db.exec(`ALTER TABLE products ADD COLUMN servings_json TEXT`)
  }
}

/** Old DBs had CHECK (source IN ('custom','openfoodfacts')); allow 'staple'. */
function migrateProductsSourceCheckIfNeeded() {
  const row = db
    .prepare(
      `SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'products'`,
    )
    .get() as { sql: string } | undefined

  if (!row?.sql) return
  if (!row.sql.includes("source IN ('custom', 'openfoodfacts')")) return
  if (row.sql.includes("'staple'")) return

  db.exec(`
    PRAGMA foreign_keys = OFF;
    BEGIN;
    CREATE TABLE products_v2 (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      brand TEXT,
      barcode TEXT,
      kcal_100g REAL NOT NULL,
      protein_100g REAL NOT NULL,
      carbs_100g REAL NOT NULL,
      fat_100g REAL NOT NULL,
      source TEXT NOT NULL,
      servings_json TEXT,
      created_at TEXT NOT NULL DEFAULT (datetime('now'))
    );
    INSERT INTO products_v2
      (id, name, brand, barcode, kcal_100g, protein_100g, carbs_100g, fat_100g, source, servings_json, created_at)
    SELECT
      id, name, brand, barcode, kcal_100g, protein_100g, carbs_100g, fat_100g, source, servings_json, created_at
    FROM products;
    DROP TABLE products;
    ALTER TABLE products_v2 RENAME TO products;
    CREATE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode);
    CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
    COMMIT;
    PRAGMA foreign_keys = ON;
  `)
}

function rowToProduct(row: Record<string, unknown>): Product {
  const source = String(row.source) as ProductSource
  return {
    id: Number(row.id),
    name: String(row.name),
    brand: row.brand == null ? null : String(row.brand),
    barcode: row.barcode == null ? null : String(row.barcode),
    kcal_100g: Number(row.kcal_100g),
    protein_100g: Number(row.protein_100g),
    carbs_100g: Number(row.carbs_100g),
    fat_100g: Number(row.fat_100g),
    source:
      source === 'staple' || source === 'openfoodfacts' || source === 'custom'
        ? source
        : 'custom',
    servings: parseServings(row.servings_json),
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
  source?: ProductSource
  servings?: ServingOption[]
}): Product {
  const servings = input.servings?.length ? input.servings : defaultServings()
  const result = db
    .prepare(
      `INSERT INTO products
        (name, brand, barcode, kcal_100g, protein_100g, carbs_100g, fat_100g, source, servings_json)
       VALUES (@name, @brand, @barcode, @kcal_100g, @protein_100g, @carbs_100g, @fat_100g, @source, @servings_json)`,
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
      servings_json: JSON.stringify(servings),
    })

  const row = db
    .prepare(`SELECT * FROM products WHERE id = ?`)
    .get(result.lastInsertRowid) as Record<string, unknown>
  return rowToProduct(row)
}

/** Most frequently logged products for a meal (count desc, then recency). */
export function listFrequentProductsForMeal(
  meal: MealId,
  limit = 30,
): Product[] {
  const rows = db
    .prepare(
      `SELECT p.*, COUNT(*) AS use_count, MAX(e.created_at) AS last_used
       FROM products p
       JOIN diary_entries e ON e.product_id = p.id
       WHERE e.meal = ?
       GROUP BY p.id
       ORDER BY use_count DESC, last_used DESC
       LIMIT ?`,
    )
    .all(meal, limit) as Array<Record<string, unknown>>
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

export function findByBarcode(barcode: string): Product | null {
  const code = barcode.replace(/\D/g, '')
  if (!code) return null
  const row = db
    .prepare(`SELECT * FROM products WHERE barcode = ? ORDER BY id DESC LIMIT 1`)
    .get(code) as Record<string, unknown> | undefined
  return row ? rowToProduct(row) : null
}

export function searchLocalProducts(query: string, limit = 30): Product[] {
  const q = query.trim()
  if (q.length < 2) return []
  const rows = db
    .prepare(
      `SELECT * FROM products
       WHERE name LIKE ? OR IFNULL(brand, '') LIKE ? OR IFNULL(barcode, '') LIKE ?
       ORDER BY
         CASE source WHEN 'staple' THEN 0 WHEN 'custom' THEN 1 ELSE 2 END,
         CASE WHEN lower(name) = lower(?) THEN 0 WHEN lower(name) LIKE lower(?) THEN 1 ELSE 2 END,
         name ASC
       LIMIT ?`,
    )
    .all(`%${q}%`, `%${q}%`, `%${q}%`, q, `${q}%`, limit) as Array<
    Record<string, unknown>
  >
  return rows.map(rowToProduct)
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

function entryFromJoinRow(row: Record<string, unknown>): DiaryEntry {
  return {
    id: Number(row.entry_id),
    entry_date: String(row.entry_date),
    meal: row.meal as MealId,
    grams: Number(row.grams),
    created_at: String(row.entry_created_at),
    product: rowToProduct({
      id: row.product_id,
      name: row.name,
      brand: row.brand,
      barcode: row.barcode,
      kcal_100g: row.kcal_100g,
      protein_100g: row.protein_100g,
      carbs_100g: row.carbs_100g,
      fat_100g: row.fat_100g,
      source: row.source,
      servings_json: row.servings_json,
      created_at: row.product_created_at,
    }),
  }
}

const ENTRY_JOIN_SQL = `
  SELECT
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
    p.servings_json,
    p.created_at AS product_created_at
  FROM diary_entries e
  JOIN products p ON p.id = e.product_id
`

export function updateDiaryEntryGrams(
  id: number,
  grams: number,
): DiaryEntry | null {
  if (!(grams > 0)) return null
  const result = db
    .prepare(`UPDATE diary_entries SET grams = ? WHERE id = ?`)
    .run(grams, id)
  if (result.changes === 0) return null

  const row = db
    .prepare(`${ENTRY_JOIN_SQL} WHERE e.id = ?`)
    .get(id) as Record<string, unknown> | undefined

  return row ? entryFromJoinRow(row) : null
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
    .prepare(`${ENTRY_JOIN_SQL} WHERE e.entry_date = ? ORDER BY e.id ASC`)
    .all(entryDate) as Array<Record<string, unknown>>

  return {
    date: entryDate,
    goals: getGoals(),
    entries: rows.map(entryFromJoinRow),
  }
}
