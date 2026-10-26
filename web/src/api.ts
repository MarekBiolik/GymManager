import type { DayData, DiaryEntry, Goals, MealId, Product } from './types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try {
      const body = (await res.json()) as { error?: string }
      if (body.error) message = body.error
    } catch {
      // ignore
    }
    throw new Error(message)
  }

  if (res.status === 204) {
    return undefined as T
  }

  return (await res.json()) as T
}

export function fetchDay(date?: string): Promise<DayData> {
  const query = date ? `?date=${encodeURIComponent(date)}` : ''
  return request<DayData>(`/api/day${query}`)
}

export type WeekDaySummary = {
  date: string
  kcal: number
  hasEntries: boolean
}

export type WeekSummary = {
  start: string
  end: string
  days: WeekDaySummary[]
}

export function fetchWeek(startDate: string): Promise<WeekSummary> {
  return request<WeekSummary>(
    `/api/week?start=${encodeURIComponent(startDate)}`,
  )
}

export function fetchFrequentProducts(meal: MealId): Promise<Product[]> {
  return request<Product[]>(
    `/api/products/frequent?meal=${encodeURIComponent(meal)}`,
  )
}

export function fetchMyProducts(): Promise<Product[]> {
  return request<Product[]>('/api/products/mine')
}

export type FoodCandidate = {
  name: string
  brand: string | null
  barcode: string | null
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  source: 'openfoodfacts'
  completeness: number | null
  servings?: import('./types').ServingOption[]
}

export type SearchFoodsResult = {
  local: Product[]
  remote: FoodCandidate[]
  warning?: string
}

export type BarcodeLookupResult =
  | { found: true; origin: 'local'; product: Product }
  | { found: true; origin: 'openfoodfacts'; product: FoodCandidate }
  | { found: false; barcode: string }

export function createProduct(input: {
  name: string
  brand?: string
  barcode?: string
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
  source?: 'custom' | 'openfoodfacts'
}): Promise<Product> {
  return request<Product>('/api/products', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function searchFoods(query: string): Promise<SearchFoodsResult> {
  return request<SearchFoodsResult>(
    `/api/foods/search?q=${encodeURIComponent(query)}`,
  )
}

export function lookupBarcode(code: string): Promise<BarcodeLookupResult> {
  return request<BarcodeLookupResult>(
    `/api/foods/barcode/${encodeURIComponent(code)}`,
  )
}

export function createEntry(input: {
  entry_date?: string
  meal: MealId
  product_id: number
  grams: number
}): Promise<DiaryEntry> {
  return request<DiaryEntry>('/api/entries', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updateEntryGrams(
  id: number,
  grams: number,
): Promise<DiaryEntry> {
  return request<DiaryEntry>(`/api/entries/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ grams }),
  })
}

export function deleteEntry(id: number): Promise<void> {
  return request<void>(`/api/entries/${id}`, { method: 'DELETE' })
}

export function updateGoals(goals: Goals): Promise<Goals> {
  return request<Goals>('/api/goals', {
    method: 'PUT',
    body: JSON.stringify(goals),
  })
}
