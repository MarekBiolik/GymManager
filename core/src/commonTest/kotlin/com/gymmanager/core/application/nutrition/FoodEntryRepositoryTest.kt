package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.AmountUnit
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodEntry
import com.gymmanager.core.domain.FoodServing
import com.gymmanager.core.domain.GoalProgress
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.domain.NutritionSettings
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.InMemoryReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class FoodEntryRepositoryTest {
    private val userId = "user-a"
    private val day = "2026-10-30"

    @Test
    fun addEntry_snapsMacrosFromFoodPer100g() =
        runTest {
            val foodRemote =
                EntryFakeFoodRemote(
                    mutableListOf(
                        food("f1", proteinG = 20.0, carbsG = 10.0, fatG = 5.0, defaultServingG = 100.0),
                    ),
                )
            val entryRemote = EntryFakeFoodEntryRemote()
            val repo = repo(entryRemote, foodRemote, EntryFakeFoodServingRemote(), online = true, now = { 99L })

            val saved =
                assertIs<WriteResult.Success<FoodEntry>>(
                    repo.addEntry(
                        userId = userId,
                        foodId = "f1",
                        mealTypeId = "meal-1",
                        eatenOn = day,
                        amount = 50.0,
                        unit = AmountUnit.G,
                        foodServingId = null,
                    ),
                )
            assertEquals(50.0, saved.data.grams)
            assertEquals(10.0, saved.data.macros.proteinG)
            assertEquals(5.0, saved.data.macros.carbsG)
            assertEquals(2.5, saved.data.macros.fatG)
            assertEquals(1, entryRemote.rows.size)
            assertEquals(99L, foodRemote.rows.single().lastUsedAtEpochMs)
        }

    @Test
    fun addEntry_servingUnitUsesNamedServing() =
        runTest {
            val foodRemote = EntryFakeFoodRemote(mutableListOf(food("f1", defaultServingG = 100.0)))
            val servingRemote =
                EntryFakeFoodServingRemote(mutableListOf(serving("s1", "f1", grams = 30.0)))
            val repo =
                repo(
                    EntryFakeFoodEntryRemote(),
                    foodRemote,
                    servingRemote,
                    online = true,
                )
            val saved =
                assertIs<WriteResult.Success<FoodEntry>>(
                    repo.addEntry(
                        userId = userId,
                        foodId = "f1",
                        mealTypeId = "meal-1",
                        eatenOn = day,
                        amount = 2.0,
                        unit = AmountUnit.SERVING,
                        foodServingId = "s1",
                    ),
                )
            assertEquals(60.0, saved.data.grams)
        }

    @Test
    fun sumMacros_andGoalProgress() =
        runTest {
            val entries =
                listOf(
                    entry(Macros(10.0, 20.0, 5.0, 165.0)),
                    entry(Macros(10.0, 0.0, 0.0, 40.0)),
                )
            val repo =
                repo(
                    EntryFakeFoodEntryRemote(),
                    EntryFakeFoodRemote(),
                    EntryFakeFoodServingRemote(),
                    online = true,
                )
            val sum = repo.sumMacros(entries)
            assertEquals(20.0, sum.proteinG)
            assertEquals(20.0, sum.carbsG)
            assertEquals(5.0, sum.fatG)

            val goals =
                NutritionSettings(
                    userId = userId,
                    calorieTargetKcal = 205.0,
                    proteinTargetG = 40.0,
                    carbsTargetG = 40.0,
                    fatTargetG = 10.0,
                    heightCm = null,
                    waterTargetMl = null,
                    createdAtEpochMs = 0,
                    updatedAtEpochMs = 0,
                )
            val progress = GoalProgress.of(sum, goals)
            assertEquals(0.5, progress.protein)
            assertEquals(0.5, progress.carbs)
            assertEquals(0.5, progress.fat)
            assertEquals(1.0, progress.calories)
        }

    @Test
    fun listDay_cachesAndOfflineServesStale() =
        runTest {
            val cache = InMemoryReadCache()
            val entryRemote =
                EntryFakeFoodEntryRemote(
                    mutableListOf(entry(Macros(1.0, 0.0, 0.0, 4.0), id = "e1")),
                )
            val online =
                repo(entryRemote, EntryFakeFoodRemote(), EntryFakeFoodServingRemote(), cache, online = true, now = { 10L })
            val fresh = assertIs<ReadResult.Fresh<List<FoodEntry>>>(online.listDay(userId, day))
            assertEquals(1, fresh.data.size)

            val offline =
                repo(entryRemote, EntryFakeFoodRemote(), EntryFakeFoodServingRemote(), cache, online = false, now = { 20L })
            val stale = assertIs<ReadResult.Stale<List<FoodEntry>>>(offline.listDay(userId, day))
            assertEquals("e1", stale.data.single().id)
            assertEquals(10L, stale.fetchedAtEpochMs)
        }

    @Test
    fun addEntry_invalidatesDayCache() =
        runTest {
            val cache = InMemoryReadCache()
            val foodRemote = EntryFakeFoodRemote(mutableListOf(food("f1")))
            val entryRemote = EntryFakeFoodEntryRemote()
            val repo = repo(entryRemote, foodRemote, EntryFakeFoodServingRemote(), cache, online = true)
            assertIs<ReadResult.Fresh<*>>(repo.listDay(userId, day))
            assertIs<WriteResult.Success<*>>(
                repo.addEntry(userId, "f1", "meal-1", day, 100.0, AmountUnit.G, null),
            )
            assertNull(cache.get<List<FoodEntry>>(CacheKeys.foodEntriesDay(userId, day)))
        }

    @Test
    fun offlineAddRejected() =
        runTest {
            val repo =
                repo(
                    EntryFakeFoodEntryRemote(),
                    EntryFakeFoodRemote(mutableListOf(food("f1"))),
                    EntryFakeFoodServingRemote(),
                    online = false,
                )
            assertIs<WriteResult.OfflineRejected>(
                repo.addEntry(userId, "f1", "meal-1", day, 100.0, AmountUnit.G, null),
            )
        }

    private fun repo(
        entryRemote: EntryFakeFoodEntryRemote,
        foodRemote: EntryFakeFoodRemote,
        servingRemote: EntryFakeFoodServingRemote,
        cache: InMemoryReadCache = InMemoryReadCache(),
        online: Boolean,
        now: () -> Long = { 10L },
    ) = FoodEntryRepository(
        remote = entryRemote,
        foodRemote = foodRemote,
        servingRemote = servingRemote,
        cache = cache,
        network = NetworkStatus { online },
        nowEpochMs = now,
    )

    private fun food(
        id: String,
        proteinG: Double = 10.0,
        carbsG: Double = 20.0,
        fatG: Double = 5.0,
        defaultServingG: Double? = 100.0,
    ) = Food(
        id = id,
        userId = userId,
        name = "Food $id",
        macrosPer100g = Macros(proteinG, carbsG, fatG),
        defaultServingG = defaultServingG,
        isRecipe = false,
        isFavorite = false,
        lastUsedAtEpochMs = null,
        externalSource = null,
        externalId = null,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )

    private fun serving(
        id: String,
        foodId: String,
        grams: Double,
    ) = FoodServing(
        id = id,
        userId = userId,
        foodId = foodId,
        label = "Serving",
        grams = grams,
        position = 0,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )

    private fun entry(
        macros: Macros,
        id: String = "e",
    ) = FoodEntry(
        id = id,
        userId = userId,
        foodId = "f1",
        mealTypeId = "meal-1",
        eatenOn = day,
        amount = 100.0,
        unit = AmountUnit.G,
        foodServingId = null,
        grams = 100.0,
        macros = macros,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )
}

private class EntryFakeFoodRemote(
    val rows: MutableList<Food> = mutableListOf(),
) : FoodRemote {
    override suspend fun listForUser(userId: String): List<Food> = rows.filter { it.userId == userId }

    override suspend fun getById(id: String): Food? = rows.firstOrNull { it.id == id }

    override suspend fun findByExternal(
        userId: String,
        source: String,
        externalId: String,
    ): Food? = null

    override suspend fun insert(food: Food): Food {
        val saved = food.copy(id = food.id.ifBlank { "generated-${rows.size}" })
        rows += saved
        return saved
    }

    override suspend fun update(food: Food): Food {
        val idx = rows.indexOfFirst { it.id == food.id }
        require(idx >= 0)
        rows[idx] = food
        return food
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }
}

private class EntryFakeFoodServingRemote(
    private val rows: MutableList<FoodServing> = mutableListOf(),
) : FoodServingRemote {
    override suspend fun listForFood(foodId: String): List<FoodServing> =
        rows.filter { it.foodId == foodId }

    override suspend fun insert(serving: FoodServing): FoodServing = serving

    override suspend fun update(serving: FoodServing): FoodServing = serving

    override suspend fun delete(id: String) {}
}

private class EntryFakeFoodEntryRemote(
    val rows: MutableList<FoodEntry> = mutableListOf(),
) : FoodEntryRemote {
    override suspend fun listForDay(
        userId: String,
        eatenOn: String,
    ): List<FoodEntry> = rows.filter { it.userId == userId && it.eatenOn == eatenOn }

    override suspend fun listForRange(
        userId: String,
        fromInclusive: String,
        toInclusive: String,
    ): List<FoodEntry> =
        rows.filter { it.userId == userId && it.eatenOn >= fromInclusive && it.eatenOn <= toInclusive }

    override suspend fun insert(entry: FoodEntry): FoodEntry {
        val saved = entry.copy(id = entry.id.ifBlank { "generated-${rows.size}" })
        rows += saved
        return saved
    }

    override suspend fun update(entry: FoodEntry): FoodEntry {
        val idx = rows.indexOfFirst { it.id == entry.id }
        require(idx >= 0)
        rows[idx] = entry
        return entry
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }
}
