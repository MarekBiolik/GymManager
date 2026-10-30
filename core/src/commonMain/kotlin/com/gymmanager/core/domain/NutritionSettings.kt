package com.gymmanager.core.domain

/**
 * Per-user nutrition goals and related body/water targets.
 *
 * Units (MVP, see nutrition-module spec): energy **kcal**, macros **g**,
 * height **cm**, water **ml**. Mass elsewhere uses **kg** / **g** only —
 * no imperial lb/oz in this domain slice.
 */
data class NutritionSettings(
    val userId: String,
    val calorieTargetKcal: Double,
    val proteinTargetG: Double,
    val carbsTargetG: Double,
    val fatTargetG: Double,
    val heightCm: Double?,
    val waterTargetMl: Double?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/**
 * Editable diary section for a user (ordered by [position]).
 */
data class MealType(
    val id: String,
    val userId: String,
    val name: String,
    val position: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/** User-owned food catalog item; macros stored per 100 g. */
data class Food(
    val id: String,
    val userId: String,
    val name: String,
    val macrosPer100g: Macros,
    val defaultServingG: Double?,
    val isRecipe: Boolean,
    val isFavorite: Boolean,
    val lastUsedAtEpochMs: Long?,
    val externalSource: String?,
    val externalId: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

/** Named serving size for a [Food] (grams per serving). */
data class FoodServing(
    val id: String,
    val userId: String,
    val foodId: String,
    val label: String,
    val grams: Double,
    val position: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
