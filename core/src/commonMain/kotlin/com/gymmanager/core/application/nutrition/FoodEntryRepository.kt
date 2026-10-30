package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.AmountUnit
import com.gymmanager.core.domain.FoodEntry
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.resolveGrams
import com.gymmanager.core.sync.CacheEntry
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult

class FoodEntryRepository(
    private val remote: FoodEntryRemote,
    private val foodRemote: FoodRemote,
    private val servingRemote: FoodServingRemote,
    private val cache: ReadCache,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun listDay(
        userId: String,
        eatenOn: String,
    ): ReadResult<List<FoodEntry>> {
        if (!network.isOnline()) {
            val hit = cache.get<List<FoodEntry>>(CacheKeys.foodEntriesDay(userId, eatenOn))
            return if (hit != null && hit.userId == userId) {
                ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
            } else {
                ReadResult.Empty(EmptyReason.OFFLINE_MISS)
            }
        }
        return try {
            val list = remote.listForDay(userId, eatenOn)
            cache.put(
                CacheKeys.foodEntriesDay(userId, eatenOn),
                CacheEntry(list, nowEpochMs(), userId),
            )
            ReadResult.Fresh(list)
        } catch (_: Exception) {
            val hit = cache.get<List<FoodEntry>>(CacheKeys.foodEntriesDay(userId, eatenOn))
            if (hit != null && hit.userId == userId) {
                ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
            } else {
                ReadResult.Empty(EmptyReason.ERROR_MISS)
            }
        }
    }

    suspend fun listRange(
        userId: String,
        fromInclusive: String,
        toInclusive: String,
    ): ReadResult<List<FoodEntry>> {
        if (!network.isOnline()) return ReadResult.Empty(EmptyReason.OFFLINE_MISS)
        return try {
            ReadResult.Fresh(remote.listForRange(userId, fromInclusive, toInclusive))
        } catch (_: Exception) {
            ReadResult.Empty(EmptyReason.ERROR_MISS)
        }
    }

    suspend fun addEntry(
        userId: String,
        foodId: String,
        mealTypeId: String,
        eatenOn: String,
        amount: Double,
        unit: AmountUnit,
        foodServingId: String?,
    ): WriteResult<FoodEntry> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val food = foodRemote.getById(foodId) ?: return WriteResult.Failed("Food not found")
            val serving =
                foodServingId?.let { id ->
                    servingRemote.listForFood(foodId).firstOrNull { it.id == id }
                }
            val grams =
                resolveGrams(
                    amount = amount,
                    unit = unit,
                    servingGrams = serving?.grams,
                    defaultServingG = food.defaultServingG,
                )
            val macros = Macros.per100gScaled(food.macrosPer100g, grams)
            val now = nowEpochMs()
            val entry =
                FoodEntry(
                    id = "",
                    userId = userId,
                    foodId = foodId,
                    mealTypeId = mealTypeId,
                    eatenOn = eatenOn,
                    amount = amount,
                    unit = unit,
                    foodServingId = foodServingId,
                    grams = grams,
                    macros = macros,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                )
            val saved = remote.insert(entry)
            foodRemote.update(food.copy(lastUsedAtEpochMs = now, updatedAtEpochMs = now))
            cache.remove(CacheKeys.foodEntriesDay(userId, eatenOn))
            cache.remove(CacheKeys.foodsList(userId))
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Add entry failed")
        }
    }

    suspend fun deleteEntry(
        userId: String,
        eatenOn: String,
        id: String,
    ): WriteResult<Unit> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            remote.delete(id)
            cache.remove(CacheKeys.foodEntriesDay(userId, eatenOn))
            WriteResult.Success(Unit)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Delete failed")
        }
    }

    suspend fun copyDay(
        userId: String,
        fromDate: String,
        toDate: String,
    ): WriteResult<List<FoodEntry>> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val source = remote.listForDay(userId, fromDate)
            val now = nowEpochMs()
            val copied =
                source.map { entry ->
                    remote.insert(
                        entry.copy(
                            id = "",
                            eatenOn = toDate,
                            createdAtEpochMs = now,
                            updatedAtEpochMs = now,
                        ),
                    )
                }
            cache.remove(CacheKeys.foodEntriesDay(userId, toDate))
            WriteResult.Success(copied)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Copy day failed")
        }
    }

    suspend fun copyMeal(
        userId: String,
        fromDate: String,
        toDate: String,
        mealTypeId: String,
        targetMealTypeId: String = mealTypeId,
    ): WriteResult<List<FoodEntry>> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val source =
                remote.listForDay(userId, fromDate).filter { it.mealTypeId == mealTypeId }
            val now = nowEpochMs()
            val copied =
                source.map { entry ->
                    remote.insert(
                        entry.copy(
                            id = "",
                            eatenOn = toDate,
                            mealTypeId = targetMealTypeId,
                            createdAtEpochMs = now,
                            updatedAtEpochMs = now,
                        ),
                    )
                }
            cache.remove(CacheKeys.foodEntriesDay(userId, toDate))
            WriteResult.Success(copied)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Copy meal failed")
        }
    }

    fun sumMacros(entries: List<FoodEntry>): Macros =
        entries.fold(Macros.zero()) { acc, e -> acc + e.macros }
}
