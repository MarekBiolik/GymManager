package com.gymmanager.core.data.off

import com.gymmanager.core.domain.Macros
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OffMappingTest {
    @Test
    fun toFoodDraft_setsProvenance() {
        val candidate =
            OffProductCandidate(
                code = "123",
                name = "Yoghurt",
                macrosPer100g = Macros(4.0, 5.0, 3.0),
                defaultServingG = 125.0,
            )
        val food = candidate.toFoodDraft("user-1", nowEpochMs = 1000L)
        assertEquals("openfoodfacts", food.externalSource)
        assertEquals("123", food.externalId)
        assertEquals("Yoghurt", food.name)
        assertEquals(125.0, food.defaultServingG)
        assertEquals(4.0, food.macrosPer100g.proteinG)
        assertFalse(food.isRecipe)
    }
}
