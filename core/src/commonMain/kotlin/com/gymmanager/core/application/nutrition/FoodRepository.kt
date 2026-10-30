package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodServing
import com.gymmanager.core.sync.CacheEntry
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult

class FoodRepository(
    private val remote: FoodRemote,
    private val servingRemote: FoodServingRemote,
    private val cache: ReadCache,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun list(userId: String): ReadResult<List<Food>> {
        if (!network.isOnline()) {
            val hit = cache.get<List<Food>>(CacheKeys.foodsList(userId))
            return if (hit != null && hit.userId == userId) {
                ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
            } else {
                ReadResult.Empty(EmptyReason.OFFLINE_MISS)
            }
        }
        return try {
            val list =
                remote.listForUser(userId).sortedWith(
                    compareByDescending<Food> { it.isFavorite }
                        .thenByDescending { it.lastUsedAtEpochMs ?: 0L }
                        .thenBy { it.name.lowercase() },
                )
            cache.put(CacheKeys.foodsList(userId), CacheEntry(list, nowEpochMs(), userId))
            ReadResult.Fresh(list)
        } catch (_: Exception) {
            val hit = cache.get<List<Food>>(CacheKeys.foodsList(userId))
            if (hit != null && hit.userId == userId) {
                ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
            } else {
                ReadResult.Empty(EmptyReason.ERROR_MISS)
            }
        }
    }

    suspend fun listServings(foodId: String): ReadResult<List<FoodServing>> {
        if (!network.isOnline()) return ReadResult.Empty(EmptyReason.OFFLINE_MISS)
        return try {
            ReadResult.Fresh(servingRemote.listForFood(foodId).sortedBy { it.position })
        } catch (_: Exception) {
            ReadResult.Empty(EmptyReason.ERROR_MISS)
        }
    }

    suspend fun upsertFood(food: Food): WriteResult<Food> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val saved =
                if (food.id.isBlank()) {
                    remote.insert(food.copy(updatedAtEpochMs = nowEpochMs()))
                } else {
                    remote.update(food.copy(updatedAtEpochMs = nowEpochMs()))
                }
            cache.remove(CacheKeys.foodsList(food.userId))
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Food save failed")
        }
    }

    suspend fun deleteFood(
        userId: String,
        id: String,
    ): WriteResult<Unit> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            remote.delete(id)
            cache.remove(CacheKeys.foodsList(userId))
            WriteResult.Success(Unit)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Delete failed")
        }
    }

    suspend fun upsertServing(serving: FoodServing): WriteResult<FoodServing> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val saved =
                if (serving.id.isBlank()) {
                    servingRemote.insert(serving)
                } else {
                    servingRemote.update(serving.copy(updatedAtEpochMs = nowEpochMs()))
                }
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Serving save failed")
        }
    }

    suspend fun deleteServing(id: String): WriteResult<Unit> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            servingRemote.delete(id)
            WriteResult.Success(Unit)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Delete failed")
        }
    }

    suspend fun markUsed(food: Food): WriteResult<Food> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return upsertFood(food.copy(lastUsedAtEpochMs = nowEpochMs()))
    }

    suspend fun findByExternal(
        userId: String,
        source: String,
        externalId: String,
    ): Food? {
        if (!network.isOnline()) return null
        return try {
            remote.findByExternal(userId, source, externalId)
        } catch (_: Exception) {
            null
        }
    }
}
