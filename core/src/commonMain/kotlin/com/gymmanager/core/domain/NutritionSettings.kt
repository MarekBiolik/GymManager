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
