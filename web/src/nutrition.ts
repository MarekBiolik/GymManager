import type { DiaryEntry, Goals, MealId } from './types'

export type Macros = {
  kcal: number
  protein: number
  carbs: number
  fat: number
}

type NutritionPer100g = {
  kcal_100g: number
  protein_100g: number
  carbs_100g: number
  fat_100g: number
}

export function macrosForGrams(product: NutritionPer100g, grams: number): Macros {
  const factor = grams / 100
  return {
    kcal: product.kcal_100g * factor,
    protein: product.protein_100g * factor,
    carbs: product.carbs_100g * factor,
    fat: product.fat_100g * factor,
  }
}

export function sumMacros(entries: DiaryEntry[]): Macros {
  return entries.reduce<Macros>(
    (acc, entry) => {
      const m = macrosForGrams(entry.product, entry.grams)
      return {
        kcal: acc.kcal + m.kcal,
        protein: acc.protein + m.protein,
        carbs: acc.carbs + m.carbs,
        fat: acc.fat + m.fat,
      }
    },
    { kcal: 0, protein: 0, carbs: 0, fat: 0 },
  )
}

export function mealMacros(entries: DiaryEntry[], meal: MealId): Macros {
  return sumMacros(entries.filter((entry) => entry.meal === meal))
}

export function remaining(goals: Goals, eaten: Macros): Macros {
  return {
    kcal: goals.kcal - eaten.kcal,
    protein: goals.protein - eaten.protein,
    carbs: goals.carbs - eaten.carbs,
    fat: goals.fat - eaten.fat,
  }
}

export function progress(eaten: number, goal: number): number {
  if (goal <= 0) return 0
  return Math.min(100, Math.max(0, (eaten / goal) * 100))
}

export function round1(n: number): string {
  return (Math.round(n * 10) / 10).toFixed(n % 1 === 0 ? 0 : 1)
}

export function round0(n: number): string {
  return String(Math.round(n))
}
