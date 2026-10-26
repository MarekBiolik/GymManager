export type MealId =
  | 'breakfast_1'
  | 'breakfast_2'
  | 'lunch'
  | 'snack'
  | 'dinner'

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

export type DayData = {
  date: string
  goals: Goals
  entries: DiaryEntry[]
}

export const MEAL_ORDER: MealId[] = [
  'breakfast_1',
  'breakfast_2',
  'lunch',
  'snack',
  'dinner',
]
