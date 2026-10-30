package com.gymmanager.core.data.supabase

import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.NutritionSettings
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
