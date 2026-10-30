package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.NutritionSettings
import com.gymmanager.core.sync.CacheEntry
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult

class NutritionSettingsRepository(
    private val remote: NutritionSettingsRemote,
    private val cache: ReadCache,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun read(userId: String): ReadResult<NutritionSettings> {
        require(userId.isNotBlank())
        if (!network.isOnline()) {
            return staleOrEmpty(userId, EmptyReason.OFFLINE_MISS)
        }
        return try {
            val fresh = remote.fetch(userId)
            if (fresh == null) {
                cache.remove(CacheKeys.nutritionSettings(userId))
                ReadResult.Empty(EmptyReason.NOT_FOUND)
            } else {
                cache.put(
                    CacheKeys.nutritionSettings(userId),
                    CacheEntry(fresh, nowEpochMs(), userId),
                )
                ReadResult.Fresh(fresh)
            }
        } catch (_: Exception) {
            staleOrEmpty(userId, EmptyReason.ERROR_MISS)
        }
    }

    suspend fun upsert(settings: NutritionSettings): WriteResult<NutritionSettings> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val saved = remote.upsert(settings.copy(updatedAtEpochMs = nowEpochMs()))
            cache.put(
                CacheKeys.nutritionSettings(settings.userId),
                CacheEntry(saved, nowEpochMs(), settings.userId),
            )
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Upsert failed")
        }
    }

    private fun staleOrEmpty(
        userId: String,
        miss: EmptyReason,
    ): ReadResult<NutritionSettings> {
        val hit = cache.get<NutritionSettings>(CacheKeys.nutritionSettings(userId))
        return if (hit != null && hit.userId == userId) {
            ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
        } else {
            if (hit != null && hit.userId != userId) {
                cache.remove(CacheKeys.nutritionSettings(userId))
            }
            ReadResult.Empty(miss)
        }
    }
}

class MealTypeRepository(
    private val remote: MealTypeRemote,
    private val cache: ReadCache,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun list(userId: String): ReadResult<List<MealType>> {
        if (!network.isOnline()) {
            val hit = cache.get<List<MealType>>(CacheKeys.mealTypesList(userId))
            return if (hit != null && hit.userId == userId) {
                ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
            } else {
                ReadResult.Empty(EmptyReason.OFFLINE_MISS)
            }
        }
        return try {
            val list = remote.listForUser(userId).sortedBy { it.position }
            cache.put(
                CacheKeys.mealTypesList(userId),
                CacheEntry(list, nowEpochMs(), userId),
            )
            ReadResult.Fresh(list)
        } catch (_: Exception) {
            val hit = cache.get<List<MealType>>(CacheKeys.mealTypesList(userId))
            if (hit != null && hit.userId == userId) {
                ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
            } else {
                ReadResult.Empty(EmptyReason.ERROR_MISS)
            }
        }
    }

    suspend fun insert(
        userId: String,
        name: String,
        position: Int,
    ): WriteResult<MealType> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        val now = nowEpochMs()
        val draft =
            MealType(
                id = "",
                userId = userId,
                name = name.trim(),
                position = position,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            )
        return try {
            val saved = remote.insert(draft)
            invalidateList(userId)
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Insert failed")
        }
    }

    suspend fun update(mealType: MealType): WriteResult<MealType> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val saved = remote.update(mealType.copy(updatedAtEpochMs = nowEpochMs()))
            invalidateList(mealType.userId)
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Update failed")
        }
    }

    suspend fun delete(
        userId: String,
        id: String,
    ): WriteResult<Unit> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            remote.delete(id)
            invalidateList(userId)
            WriteResult.Success(Unit)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Delete failed")
        }
    }

    private fun invalidateList(userId: String) {
        cache.remove(CacheKeys.mealTypesList(userId))
    }
}
