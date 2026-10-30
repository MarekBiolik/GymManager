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

/** Diary row with snapped macros at log time. */
data class FoodEntry(
    val id: String,
    val userId: String,
    val foodId: String?,
    val mealTypeId: String,
    /** Local calendar day as ISO-8601 date string YYYY-MM-DD. */
    val eatenOn: String,
    val amount: Double,
    val unit: AmountUnit,
    val foodServingId: String?,
    val grams: Double,
    val macros: Macros,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

data class DailyMacroTotals(
    val eatenOn: String,
    val macros: Macros,
)

data class GoalProgress(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    companion object {
        fun of(
            consumed: Macros,
            goals: NutritionSettings,
        ): GoalProgress {
            fun ratio(
                value: Double,
                target: Double,
            ): Double = if (target <= 0.0) 0.0 else (value / target).coerceAtLeast(0.0)

            return GoalProgress(
                calories = ratio(consumed.kcal, goals.calorieTargetKcal),
                protein = ratio(consumed.proteinG, goals.proteinTargetG),
                carbs = ratio(consumed.carbsG, goals.carbsTargetG),
                fat = ratio(consumed.fatG, goals.fatTargetG),
            )
        }
    }
}
