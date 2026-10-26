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

export function fetchRecentProducts(): Promise<Product[]> {
  return request<Product[]>('/api/products/recent')
}

export function fetchMyProducts(): Promise<Product[]> {
  return request<Product[]>('/api/products/mine')
}

export function createProduct(input: {
  name: string
  brand?: string
  barcode?: string
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
}): Promise<Product> {
  return request<Product>('/api/products', {
    method: 'POST',
    body: JSON.stringify(input),
  })
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

export function deleteEntry(id: number): Promise<void> {
  return request<void>(`/api/entries/${id}`, { method: 'DELETE' })
}

export function updateGoals(goals: Goals): Promise<Goals> {
  return request<Goals>('/api/goals', {
    method: 'PUT',
    body: JSON.stringify(goals),
  })
}
