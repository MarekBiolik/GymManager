package com.gymmanager.core.data.supabase

import com.gymmanager.core.application.nutrition.MealTypeRemote
import com.gymmanager.core.application.nutrition.NutritionSettingsRemote
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
