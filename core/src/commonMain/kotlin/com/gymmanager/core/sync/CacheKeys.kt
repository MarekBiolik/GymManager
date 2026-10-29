package com.gymmanager.core.sync

object CacheKeys {
    fun profile(userId: String): String = "profile:$userId"

    fun measurementsList(userId: String): String = "measurements:list:$userId"

    fun measurementItem(id: String): String = "measurements:item:$id"
}
