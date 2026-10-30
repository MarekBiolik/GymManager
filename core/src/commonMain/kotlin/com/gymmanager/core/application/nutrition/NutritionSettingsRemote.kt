package com.gymmanager.core.application.nutrition

import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodServing
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

interface FoodRemote {
    suspend fun listForUser(userId: String): List<Food>

    suspend fun getById(id: String): Food?

    suspend fun findByExternal(
        userId: String,
        source: String,
        externalId: String,
    ): Food?

    suspend fun insert(food: Food): Food

    suspend fun update(food: Food): Food

    suspend fun delete(id: String)
}

interface FoodServingRemote {
    suspend fun listForFood(foodId: String): List<FoodServing>

    suspend fun insert(serving: FoodServing): FoodServing

    suspend fun update(serving: FoodServing): FoodServing

    suspend fun delete(id: String)
}

interface FoodEntryRemote {
    suspend fun listForDay(
        userId: String,
        eatenOn: String,
    ): List<com.gymmanager.core.domain.FoodEntry>

    suspend fun listForRange(
        userId: String,
        fromInclusive: String,
        toInclusive: String,
    ): List<com.gymmanager.core.domain.FoodEntry>

    suspend fun insert(entry: com.gymmanager.core.domain.FoodEntry): com.gymmanager.core.domain.FoodEntry

    suspend fun update(entry: com.gymmanager.core.domain.FoodEntry): com.gymmanager.core.domain.FoodEntry

    suspend fun delete(id: String)
}

interface RecipeIngredientRemote {
    suspend fun listForRecipe(recipeFoodId: String): List<com.gymmanager.core.domain.RecipeIngredient>

    suspend fun insert(
        ingredient: com.gymmanager.core.domain.RecipeIngredient,
    ): com.gymmanager.core.domain.RecipeIngredient

    suspend fun delete(id: String)

    suspend fun deleteForRecipe(recipeFoodId: String)
}

interface WaterEntryRemote {
    suspend fun listForDay(
        userId: String,
        drunkOn: String,
    ): List<com.gymmanager.core.domain.WaterEntry>

    suspend fun insert(entry: com.gymmanager.core.domain.WaterEntry): com.gymmanager.core.domain.WaterEntry

    suspend fun delete(id: String)
}
