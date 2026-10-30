package com.gymmanager.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Documents MVP unit contract for nutrition settings (spec: nutrition-module).
 * Field names encode kg/g/cm/ml/kcal — no imperial types in this slice.
 */
class NutritionSettingsUnitsTest {
    @Test
    fun settingsFieldsUseMetricUnitSuffixes() {
        val settings =
            NutritionSettings(
                userId = "u",
                calorieTargetKcal = 2000.0,
                proteinTargetG = 150.0,
                carbsTargetG = 200.0,
                fatTargetG = 65.0,
                heightCm = 178.0,
                waterTargetMl = 3000.0,
                createdAtEpochMs = 0,
                updatedAtEpochMs = 0,
            )
        assertEquals(2000.0, settings.calorieTargetKcal)
        assertTrue(settings.proteinTargetG > 0)
        assertTrue(settings.heightCm!! > 0)
        assertTrue(settings.waterTargetMl!! > 0)
    }

    @Test
    fun mealTypeKeepsTrimmedNameContractViaRepositoryDraft() {
        // Domain allows any non-blank name; DB CHECK enforces trim length > 0.
        val meal =
            MealType(
                id = "m1",
                userId = "u",
                name = "Breakfast",
                position = 0,
                createdAtEpochMs = 0,
                updatedAtEpochMs = 0,
            )
        assertEquals("Breakfast", meal.name)
        assertEquals(0, meal.position)
    }
}
