package com.gymmanager.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MacrosTest {
    @Test
    fun derivedKcal_usesAtwaterFactors() {
        assertEquals(4.0 * 10 + 4.0 * 20 + 9.0 * 5, Macros.derivedKcal(10.0, 20.0, 5.0))
    }

    @Test
    fun per100gScaled_scalesLinearly() {
        val per100 = Macros(10.0, 20.0, 5.0)
        val scaled = Macros.per100gScaled(per100, 50.0)
        assertEquals(5.0, scaled.proteinG)
        assertEquals(10.0, scaled.carbsG)
        assertEquals(2.5, scaled.fatG)
    }

    @Test
    fun resolveGrams_servingUsesServingOrDefault() {
        assertEquals(30.0, resolveGrams(1.0, AmountUnit.SERVING, 30.0, null))
        assertEquals(50.0, resolveGrams(1.0, AmountUnit.SERVING, null, 50.0))
        assertEquals(100.0, resolveGrams(100.0, AmountUnit.G, null, null))
        assertFailsWith<IllegalStateException> {
            resolveGrams(1.0, AmountUnit.SERVING, null, null)
        }
    }

    @Test
    fun recipeMacrosPer100g_fromComponents() {
        // 200 g of mix with 20g protein total → 10g protein / 100g
        val total = Macros(20.0, 40.0, 10.0, kcal = 330.0)
        val per100 = recipeMacrosPer100g(total, 200.0)
        assertEquals(10.0, per100.proteinG)
        assertEquals(20.0, per100.carbsG)
        assertEquals(5.0, per100.fatG)
    }

    @Test
    fun goalProgress_ratios() {
        val goals =
            NutritionSettings(
                userId = "u",
                calorieTargetKcal = 2000.0,
                proteinTargetG = 100.0,
                carbsTargetG = 200.0,
                fatTargetG = 50.0,
                heightCm = null,
                waterTargetMl = null,
                createdAtEpochMs = 0,
                updatedAtEpochMs = 0,
            )
        val progress = GoalProgress.of(Macros(50.0, 100.0, 25.0, 825.0), goals)
        assertEquals(0.5, progress.protein)
        assertEquals(0.5, progress.carbs)
        assertEquals(0.5, progress.fat)
        assertEquals(825.0 / 2000.0, progress.calories)
    }

    @Test
    fun aggregations_dailyAndAverage() {
        val entries =
            listOf(
                entry("2026-01-01", Macros(10.0, 0.0, 0.0, 40.0)),
                entry("2026-01-01", Macros(0.0, 10.0, 0.0, 40.0)),
                entry("2026-01-02", Macros(20.0, 0.0, 0.0, 80.0)),
            )
        val daily = NutritionAggregations.dailyTotals(entries)
        assertEquals(2, daily.size)
        assertEquals(10.0, daily[0].macros.proteinG)
        assertEquals(10.0, daily[0].macros.carbsG)
        val avg = NutritionAggregations.averageMacros(daily)
        assertEquals(15.0, avg.proteinG)
    }

    private fun entry(
        day: String,
        macros: Macros,
    ) = FoodEntry(
        id = "1",
        userId = "u",
        foodId = "f",
        mealTypeId = "m",
        eatenOn = day,
        amount = 100.0,
        unit = AmountUnit.G,
        foodServingId = null,
        grams = 100.0,
        macros = macros,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
    )
}
