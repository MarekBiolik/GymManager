import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { createProduct, db, type ServingOption } from './db.ts'

type StapleSeed = {
  name: string
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  servings: ServingOption[]
}

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/** Idempotent seed of Polish staple foods (banana, oil spoon sizes, etc.). */
export function seedStaples(): number {
  const raw = readFileSync(path.join(__dirname, 'staples.json'), 'utf8')
  const staples = JSON.parse(raw) as StapleSeed[]

  const existing = db
    .prepare(`SELECT name FROM products WHERE source = 'staple'`)
    .all() as Array<{ name: string }>
  const have = new Set(existing.map((row) => row.name.toLowerCase()))

  let inserted = 0
  const tx = db.transaction(() => {
    for (const staple of staples) {
      if (have.has(staple.name.toLowerCase())) continue
      createProduct({
        name: staple.name,
        kcal_100g: staple.kcal_100g,
        protein_100g: staple.protein_100g,
        carbs_100g: staple.carbs_100g,
        fat_100g: staple.fat_100g,
        source: 'staple',
        servings: staple.servings,
      })
      inserted += 1
    }
  })
  tx()
  return inserted
}
