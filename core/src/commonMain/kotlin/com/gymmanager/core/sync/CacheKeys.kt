package com.gymmanager.core.sync

object CacheKeys {
    fun profile(userId: String): String = "profile:$userId"

    fun measurementsList(userId: String): String = "measurements:list:$userId"

    fun measurementItem(id: String): String = "measurements:item:$id"

    fun nutritionSettings(userId: String): String = "nutrition_settings:$userId"

    fun mealTypesList(userId: String): String = "meal_types:list:$userId"

    fun foodsList(userId: String): String = "foods:list:$userId"

    fun foodEntriesDay(
        userId: String,
        eatenOn: String,
    ): String = "food_entries:day:$userId:$eatenOn"

    fun waterDay(
        userId: String,
        drunkOn: String,
    ): String = "water:day:$userId:$drunkOn"
}
