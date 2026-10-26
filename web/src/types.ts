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

export type DayData = {
  date: string
  goals: Goals
  entries: DiaryEntry[]
}

export const MEAL_LABELS: Record<MealId, string> = {
  breakfast: 'Śniadanie',
  lunch: 'Obiad',
  dinner: 'Kolacja',
  snacks: 'Przekąski',
}

export const MEAL_ORDER: MealId[] = [
  'breakfast',
  'lunch',
  'dinner',
  'snacks',
]
