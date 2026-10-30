package com.gymmanager.core.data.supabase

import com.gymmanager.core.domain.AmountUnit
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodEntry
import com.gymmanager.core.domain.FoodServing
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.NutritionSettings
import com.gymmanager.core.domain.RecipeIngredient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NutritionSettingsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("calorie_target_kcal") val calorieTargetKcal: Double,
    @SerialName("protein_target_g") val proteinTargetG: Double,
    @SerialName("carbs_target_g") val carbsTargetG: Double,
    @SerialName("fat_target_g") val fatTargetG: Double,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("water_target_ml") val waterTargetMl: Double? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

fun NutritionSettingsDto.toDomain() =
    NutritionSettings(
        userId = userId,
        calorieTargetKcal = calorieTargetKcal,
        proteinTargetG = proteinTargetG,
        carbsTargetG = carbsTargetG,
        fatTargetG = fatTargetG,
        heightCm = heightCm,
        waterTargetMl = waterTargetMl,
        createdAtEpochMs = parseTimestamptzToEpochMs(createdAt),
        updatedAtEpochMs = parseTimestamptzToEpochMs(updatedAt),
    )

fun NutritionSettings.toDto() =
    NutritionSettingsDto(
        userId = userId,
        calorieTargetKcal = calorieTargetKcal,
        proteinTargetG = proteinTargetG,
        carbsTargetG = carbsTargetG,
        fatTargetG = fatTargetG,
        heightCm = heightCm,
        waterTargetMl = waterTargetMl,
        createdAt = epochMsToTimestamptz(createdAtEpochMs),
        updatedAt = epochMsToTimestamptz(updatedAtEpochMs),
    )

@Serializable
data class MealTypeDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val name: String,
    val position: Int,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun MealTypeDto.toDomain() =
    MealType(
        id = id ?: "",
        userId = userId,
        name = name,
        position = position,
        createdAtEpochMs = createdAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
        updatedAtEpochMs = updatedAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
    )

fun MealType.toDto() =
    MealTypeDto(
        id = id.ifBlank { null },
        userId = userId,
        name = name,
        position = position,
        createdAt = if (createdAtEpochMs > 0) epochMsToTimestamptz(createdAtEpochMs) else null,
        updatedAt = if (updatedAtEpochMs > 0) epochMsToTimestamptz(updatedAtEpochMs) else null,
    )

@Serializable
data class FoodDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    @SerialName("default_serving_g") val defaultServingG: Double? = null,
    @SerialName("is_recipe") val isRecipe: Boolean = false,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
    @SerialName("external_source") val externalSource: String? = null,
    @SerialName("external_id") val externalId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun FoodDto.toDomain() =
    Food(
        id = id ?: "",
        userId = userId,
        name = name,
        macrosPer100g = Macros(proteinG, carbsG, fatG),
        defaultServingG = defaultServingG,
        isRecipe = isRecipe,
        isFavorite = isFavorite,
        lastUsedAtEpochMs = lastUsedAt?.let { parseTimestamptzToEpochMs(it) },
        externalSource = externalSource,
        externalId = externalId,
        createdAtEpochMs = createdAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
        updatedAtEpochMs = updatedAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
    )

fun Food.toDto() =
    FoodDto(
        id = id.ifBlank { null },
        userId = userId,
        name = name,
        proteinG = macrosPer100g.proteinG,
        carbsG = macrosPer100g.carbsG,
        fatG = macrosPer100g.fatG,
        defaultServingG = defaultServingG,
        isRecipe = isRecipe,
        isFavorite = isFavorite,
        lastUsedAt = lastUsedAtEpochMs?.let { epochMsToTimestamptz(it) },
        externalSource = externalSource,
        externalId = externalId,
        createdAt = if (createdAtEpochMs > 0) epochMsToTimestamptz(createdAtEpochMs) else null,
        updatedAt = if (updatedAtEpochMs > 0) epochMsToTimestamptz(updatedAtEpochMs) else null,
    )

@Serializable
data class FoodServingDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("food_id") val foodId: String,
    val label: String,
    val grams: Double,
    val position: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun FoodServingDto.toDomain() =
    FoodServing(
        id = id ?: "",
        userId = userId,
        foodId = foodId,
        label = label,
        grams = grams,
        position = position,
        createdAtEpochMs = createdAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
        updatedAtEpochMs = updatedAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
    )

fun FoodServing.toDto() =
    FoodServingDto(
        id = id.ifBlank { null },
        userId = userId,
        foodId = foodId,
        label = label,
        grams = grams,
        position = position,
        createdAt = if (createdAtEpochMs > 0) epochMsToTimestamptz(createdAtEpochMs) else null,
        updatedAt = if (updatedAtEpochMs > 0) epochMsToTimestamptz(updatedAtEpochMs) else null,
    )

@Serializable
data class FoodEntryDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("food_id") val foodId: String? = null,
    @SerialName("meal_type_id") val mealTypeId: String,
    @SerialName("eaten_on") val eatenOn: String,
    val amount: Double,
    val unit: String,
    @SerialName("food_serving_id") val foodServingId: String? = null,
    val grams: Double,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    val kcal: Double,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun FoodEntryDto.toDomain() =
    FoodEntry(
        id = id ?: "",
        userId = userId,
        foodId = foodId,
        mealTypeId = mealTypeId,
        eatenOn = eatenOn,
        amount = amount,
        unit = AmountUnit.fromDb(unit),
        foodServingId = foodServingId,
        grams = grams,
        macros = Macros(proteinG, carbsG, fatG, kcal),
        createdAtEpochMs = createdAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
        updatedAtEpochMs = updatedAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
    )

fun FoodEntry.toDto() =
    FoodEntryDto(
        id = id.ifBlank { null },
        userId = userId,
        foodId = foodId,
        mealTypeId = mealTypeId,
        eatenOn = eatenOn,
        amount = amount,
        unit = unit.toDb(),
        foodServingId = foodServingId,
        grams = grams,
        proteinG = macros.proteinG,
        carbsG = macros.carbsG,
        fatG = macros.fatG,
        kcal = macros.kcal,
        createdAt = if (createdAtEpochMs > 0) epochMsToTimestamptz(createdAtEpochMs) else null,
        updatedAt = if (updatedAtEpochMs > 0) epochMsToTimestamptz(updatedAtEpochMs) else null,
    )

@Serializable
data class RecipeIngredientDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("recipe_food_id") val recipeFoodId: String,
    @SerialName("component_food_id") val componentFoodId: String,
    val grams: Double,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

fun RecipeIngredientDto.toDomain() =
    RecipeIngredient(
        id = id ?: "",
        userId = userId,
        recipeFoodId = recipeFoodId,
        componentFoodId = componentFoodId,
        grams = grams,
        createdAtEpochMs = createdAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
        updatedAtEpochMs = updatedAt?.let { parseTimestamptzToEpochMs(it) } ?: 0L,
    )

fun RecipeIngredient.toDto() =
    RecipeIngredientDto(
        id = id.ifBlank { null },
        userId = userId,
        recipeFoodId = recipeFoodId,
        componentFoodId = componentFoodId,
        grams = grams,
        createdAt = if (createdAtEpochMs > 0) epochMsToTimestamptz(createdAtEpochMs) else null,
        updatedAt = if (updatedAtEpochMs > 0) epochMsToTimestamptz(updatedAtEpochMs) else null,
    )
