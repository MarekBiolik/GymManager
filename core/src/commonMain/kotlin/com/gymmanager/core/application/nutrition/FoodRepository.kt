package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.AmountUnit
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodEntry
import com.gymmanager.core.domain.FoodServing
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.RecipeIngredient
import com.gymmanager.core.domain.WaterEntry
import com.gymmanager.core.domain.recipeMacrosPer100g
import com.gymmanager.core.domain.resolveGrams
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

class RecipeRepository(
    private val foodRemote: FoodRemote,
    private val ingredientRemote: RecipeIngredientRemote,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun listIngredients(recipeFoodId: String): ReadResult<List<RecipeIngredient>> {
        if (!network.isOnline()) return ReadResult.Empty(EmptyReason.OFFLINE_MISS)
        return try {
            ReadResult.Fresh(ingredientRemote.listForRecipe(recipeFoodId))
        } catch (_: Exception) {
            ReadResult.Empty(EmptyReason.ERROR_MISS)
        }
    }

    suspend fun saveRecipe(
        recipe: Food,
        ingredients: List<Pair<String, Double>>,
    ): WriteResult<Food> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        require(recipe.isRecipe)
        return try {
            var totalMacros = Macros.zero()
            var totalGrams = 0.0
            val resolved = mutableListOf<Pair<String, Double>>()
            for ((componentId, grams) in ingredients) {
                require(grams > 0.0)
                val component =
                    foodRemote.getById(componentId)
                        ?: return WriteResult.Failed("Component not found: $componentId")
                totalMacros += Macros.per100gScaled(component.macrosPer100g, grams)
                totalGrams += grams
                resolved += componentId to grams
            }
            val per100 = recipeMacrosPer100g(totalMacros, totalGrams)
            val now = nowEpochMs()
            val withMacros =
                recipe.copy(
                    macrosPer100g = per100,
                    defaultServingG = recipe.defaultServingG ?: totalGrams,
                    updatedAtEpochMs = now,
                )
            val saved =
                if (withMacros.id.isBlank()) {
                    foodRemote.insert(withMacros)
                } else {
                    ingredientRemote.deleteForRecipe(withMacros.id)
                    foodRemote.update(withMacros)
                }
            for ((componentId, grams) in resolved) {
                ingredientRemote.insert(
                    RecipeIngredient(
                        id = "",
                        userId = recipe.userId,
                        recipeFoodId = saved.id,
                        componentFoodId = componentId,
                        grams = grams,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    ),
                )
            }
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Save recipe failed")
        }
    }
}

class WaterRepository(
    private val remote: WaterEntryRemote,
    private val cache: ReadCache,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun listDay(
        userId: String,
        drunkOn: String,
    ): ReadResult<List<WaterEntry>> {
        if (!network.isOnline()) {
            val hit = cache.get<List<WaterEntry>>(CacheKeys.waterDay(userId, drunkOn))
            return if (hit != null && hit.userId == userId) {
                ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
            } else {
                ReadResult.Empty(EmptyReason.OFFLINE_MISS)
            }
        }
        return try {
            val list = remote.listForDay(userId, drunkOn)
            cache.put(CacheKeys.waterDay(userId, drunkOn), CacheEntry(list, nowEpochMs(), userId))
            ReadResult.Fresh(list)
        } catch (_: Exception) {
            ReadResult.Empty(EmptyReason.ERROR_MISS)
        }
    }

    suspend fun add(
        userId: String,
        drunkOn: String,
        amountMl: Double,
    ): WriteResult<WaterEntry> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        require(amountMl > 0.0)
        val now = nowEpochMs()
        return try {
            val saved =
                remote.insert(
                    WaterEntry(
                        id = "",
                        userId = userId,
                        drunkOn = drunkOn,
                        amountMl = amountMl,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now,
                    ),
                )
            cache.remove(CacheKeys.waterDay(userId, drunkOn))
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Add water failed")
        }
    }

    suspend fun delete(
        userId: String,
        drunkOn: String,
        id: String,
    ): WriteResult<Unit> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            remote.delete(id)
            cache.remove(CacheKeys.waterDay(userId, drunkOn))
            WriteResult.Success(Unit)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Delete failed")
        }
    }

    fun totalMl(entries: List<WaterEntry>): Double = entries.sumOf { it.amountMl }
}
