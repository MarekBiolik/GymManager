package com.gymmanager.core.application.nutrition

import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.NutritionSettings

interface NutritionSettingsRemote {
    suspend fun fetch(userId: String): NutritionSettings?

    suspend fun upsert(settings: NutritionSettings): NutritionSettings
}

interface MealTypeRemote {
    suspend fun listForUser(userId: String): List<MealType>

    suspend fun insert(mealType: MealType): MealType

    suspend fun update(mealType: MealType): MealType

    suspend fun delete(id: String)
}
