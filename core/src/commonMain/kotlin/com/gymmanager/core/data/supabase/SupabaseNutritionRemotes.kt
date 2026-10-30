package com.gymmanager.core.data.supabase

import com.gymmanager.core.application.nutrition.FoodEntryRemote
import com.gymmanager.core.application.nutrition.FoodRemote
import com.gymmanager.core.application.nutrition.FoodServingRemote
import com.gymmanager.core.application.nutrition.MealTypeRemote
import com.gymmanager.core.application.nutrition.NutritionSettingsRemote
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodEntry
import com.gymmanager.core.domain.FoodServing
import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.NutritionSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SupabaseNutritionSettingsRemote(
    private val client: SupabaseClient,
) : NutritionSettingsRemote {
    override suspend fun fetch(userId: String): NutritionSettings? =
        client
            .from("nutrition_settings")
            .select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<NutritionSettingsDto>()
            ?.toDomain()

    override suspend fun upsert(settings: NutritionSettings): NutritionSettings =
        client
            .from("nutrition_settings")
            .upsert(settings.toDto()) {
                select()
            }.decodeSingle<NutritionSettingsDto>()
            .toDomain()
}

class SupabaseMealTypeRemote(
    private val client: SupabaseClient,
) : MealTypeRemote {
    override suspend fun listForUser(userId: String): List<MealType> =
        client
            .from("meal_types")
            .select {
                filter { eq("user_id", userId) }
                order("position", Order.ASCENDING)
            }.decodeList<MealTypeDto>()
            .map { it.toDomain() }

    override suspend fun insert(mealType: MealType): MealType =
        client
            .from("meal_types")
            .insert(mealType.toDto()) { select() }
            .decodeSingle<MealTypeDto>()
            .toDomain()

    override suspend fun update(mealType: MealType): MealType =
        client
            .from("meal_types")
            .update(mealType.toDto()) {
                filter { eq("id", mealType.id) }
                select()
            }.decodeSingle<MealTypeDto>()
            .toDomain()

    override suspend fun delete(id: String) {
        client.from("meal_types").delete { filter { eq("id", id) } }
    }
}

class SupabaseFoodRemote(
    private val client: SupabaseClient,
) : FoodRemote {
    override suspend fun listForUser(userId: String): List<Food> =
        client
            .from("foods")
            .select {
                filter { eq("user_id", userId) }
                order("name", Order.ASCENDING)
            }.decodeList<FoodDto>()
            .map { it.toDomain() }

    override suspend fun getById(id: String): Food? =
        client
            .from("foods")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<FoodDto>()
            ?.toDomain()

    override suspend fun findByExternal(
        userId: String,
        source: String,
        externalId: String,
    ): Food? =
        client
            .from("foods")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("external_source", source)
                    eq("external_id", externalId)
                }
            }.decodeSingleOrNull<FoodDto>()
            ?.toDomain()

    override suspend fun insert(food: Food): Food =
        client
            .from("foods")
            .insert(food.toDto()) { select() }
            .decodeSingle<FoodDto>()
            .toDomain()

    override suspend fun update(food: Food): Food =
        client
            .from("foods")
            .update(food.toDto()) {
                filter { eq("id", food.id) }
                select()
            }.decodeSingle<FoodDto>()
            .toDomain()

    override suspend fun delete(id: String) {
        client.from("foods").delete { filter { eq("id", id) } }
    }
}

class SupabaseFoodServingRemote(
    private val client: SupabaseClient,
) : FoodServingRemote {
    override suspend fun listForFood(foodId: String): List<FoodServing> =
        client
            .from("food_servings")
            .select {
                filter { eq("food_id", foodId) }
                order("position", Order.ASCENDING)
            }.decodeList<FoodServingDto>()
            .map { it.toDomain() }

    override suspend fun insert(serving: FoodServing): FoodServing =
        client
            .from("food_servings")
            .insert(serving.toDto()) { select() }
            .decodeSingle<FoodServingDto>()
            .toDomain()

    override suspend fun update(serving: FoodServing): FoodServing =
        client
            .from("food_servings")
            .update(serving.toDto()) {
                filter { eq("id", serving.id) }
                select()
            }.decodeSingle<FoodServingDto>()
            .toDomain()

    override suspend fun delete(id: String) {
        client.from("food_servings").delete { filter { eq("id", id) } }
    }
}

class SupabaseFoodEntryRemote(
    private val client: SupabaseClient,
) : FoodEntryRemote {
    override suspend fun listForDay(
        userId: String,
        eatenOn: String,
    ): List<FoodEntry> =
        client
            .from("food_entries")
            .select {
                filter {
                    eq("user_id", userId)
                    eq("eaten_on", eatenOn)
                }
            }.decodeList<FoodEntryDto>()
            .map { it.toDomain() }

    override suspend fun listForRange(
        userId: String,
        fromInclusive: String,
        toInclusive: String,
    ): List<FoodEntry> =
        client
            .from("food_entries")
            .select {
                filter {
                    eq("user_id", userId)
                    gte("eaten_on", fromInclusive)
                    lte("eaten_on", toInclusive)
                }
                order("eaten_on", Order.ASCENDING)
            }.decodeList<FoodEntryDto>()
            .map { it.toDomain() }

    override suspend fun insert(entry: FoodEntry): FoodEntry =
        client
            .from("food_entries")
            .insert(entry.toDto()) { select() }
            .decodeSingle<FoodEntryDto>()
            .toDomain()

    override suspend fun update(entry: FoodEntry): FoodEntry =
        client
            .from("food_entries")
            .update(entry.toDto()) {
                filter { eq("id", entry.id) }
                select()
            }.decodeSingle<FoodEntryDto>()
            .toDomain()

    override suspend fun delete(id: String) {
        client.from("food_entries").delete { filter { eq("id", id) } }
    }
}
