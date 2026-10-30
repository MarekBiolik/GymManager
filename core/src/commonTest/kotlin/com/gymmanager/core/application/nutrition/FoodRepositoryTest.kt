package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.Food
import com.gymmanager.core.domain.FoodServing
import com.gymmanager.core.domain.Macros
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.InMemoryReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class FoodRepositoryTest {
    private val userId = "user-a"

    @Test
    fun listSortsFavoritesFirstAndCaches() =
        runTest {
            val cache = InMemoryReadCache()
            val foodRemote =
                FakeFoodRemote(
                    mutableListOf(
                        food("f1", "Zebra", isFavorite = false, lastUsed = null),
                        food("f2", "Apple", isFavorite = true, lastUsed = 10L),
                        food("f3", "Banana", isFavorite = true, lastUsed = 20L),
                    ),
                )
            val repo = repo(foodRemote, FakeFoodServingRemote(), cache, online = true)
            val fresh = assertIs<ReadResult.Fresh<List<Food>>>(repo.list(userId))
            assertEquals(listOf("Banana", "Apple", "Zebra"), fresh.data.map { it.name })
            assertEquals(3, cache.get<List<Food>>(CacheKeys.foodsList(userId))!!.payload.size)
        }

    @Test
    fun offlineListServesStaleCache() =
        runTest {
            val cache = InMemoryReadCache()
            val foodRemote = FakeFoodRemote(mutableListOf(food("f1", "Oats")))
            val online = repo(foodRemote, FakeFoodServingRemote(), cache, online = true, now = { 100L })
            assertIs<ReadResult.Fresh<*>>(online.list(userId))

            val offline = repo(foodRemote, FakeFoodServingRemote(), cache, online = false, now = { 200L })
            val stale = assertIs<ReadResult.Stale<List<Food>>>(offline.list(userId))
            assertEquals("Oats", stale.data.single().name)
            assertEquals(100L, stale.fetchedAtEpochMs)
        }

    @Test
    fun upsertFoodInvalidatesListCache() =
        runTest {
            val cache = InMemoryReadCache()
            val foodRemote = FakeFoodRemote(mutableListOf(food("f1", "Oats")))
            val repo = repo(foodRemote, FakeFoodServingRemote(), cache, online = true)
            assertIs<ReadResult.Fresh<*>>(repo.list(userId))
            val saved =
                assertIs<WriteResult.Success<Food>>(
                    repo.upsertFood(food("f1", "Oats", proteinG = 12.0)),
                )
            assertEquals(12.0, saved.data.macrosPer100g.proteinG)
            assertNull(cache.get<List<Food>>(CacheKeys.foodsList(userId)))
        }

    @Test
    fun offlineUpsertFoodRejected() =
        runTest {
            val repo =
                repo(
                    FakeFoodRemote(),
                    FakeFoodServingRemote(),
                    InMemoryReadCache(),
                    online = false,
                )
            assertIs<WriteResult.OfflineRejected>(repo.upsertFood(food("", "New")))
        }

    @Test
    fun listServingsSortsByPosition() =
        runTest {
            val servingRemote =
                FakeFoodServingRemote(
                    mutableListOf(
                        serving("s2", "f1", "Large", position = 2),
                        serving("s0", "f1", "Small", position = 0),
                    ),
                )
            val repo = repo(FakeFoodRemote(), servingRemote, InMemoryReadCache(), online = true)
            val fresh = assertIs<ReadResult.Fresh<List<FoodServing>>>(repo.listServings("f1"))
            assertEquals(listOf("Small", "Large"), fresh.data.map { it.label })
        }

    @Test
    fun offlineListServingsEmpty() =
        runTest {
            val repo = repo(FakeFoodRemote(), FakeFoodServingRemote(), InMemoryReadCache(), online = false)
            val empty = assertIs<ReadResult.Empty>(repo.listServings("f1"))
            assertEquals(EmptyReason.OFFLINE_MISS, empty.reason)
        }

    @Test
    fun findByExternalReturnsMatch() =
        runTest {
            val offFood =
                food("f-off", "Imported", externalSource = "openfoodfacts", externalId = "123")
            val foodRemote = FakeFoodRemote(mutableListOf(offFood))
            val repo = repo(foodRemote, FakeFoodServingRemote(), InMemoryReadCache(), online = true)
            val found = repo.findByExternal(userId, "openfoodfacts", "123")
            assertEquals("Imported", found?.name)
        }

    @Test
    fun upsertServingInsertAndUpdate() =
        runTest {
            val servingRemote = FakeFoodServingRemote()
            val repo = repo(FakeFoodRemote(), servingRemote, InMemoryReadCache(), online = true, now = { 50L })
            val inserted =
                assertIs<WriteResult.Success<FoodServing>>(
                    repo.upsertServing(serving("", "f1", "Cup", grams = 240.0)),
                )
            assertEquals("generated-0", inserted.data.id)

            val updated =
                assertIs<WriteResult.Success<FoodServing>>(
                    repo.upsertServing(inserted.data.copy(label = "Mug")),
                )
            assertEquals("Mug", updated.data.label)
            assertEquals(50L, updated.data.updatedAtEpochMs)
        }

    private fun repo(
        foodRemote: FakeFoodRemote,
        servingRemote: FakeFoodServingRemote,
        cache: InMemoryReadCache,
        online: Boolean,
        now: () -> Long = { 10L },
    ) = FoodRepository(
        remote = foodRemote,
        servingRemote = servingRemote,
        cache = cache,
        network = NetworkStatus { online },
        nowEpochMs = now,
    )

    private fun food(
        id: String,
        name: String,
        proteinG: Double = 10.0,
        isFavorite: Boolean = false,
        lastUsed: Long? = null,
        externalSource: String? = null,
        externalId: String? = null,
    ) = Food(
        id = id,
        userId = userId,
        name = name,
        macrosPer100g = Macros(proteinG, 20.0, 5.0),
        defaultServingG = 100.0,
        isRecipe = false,
        isFavorite = isFavorite,
        lastUsedAtEpochMs = lastUsed,
        externalSource = externalSource,
        externalId = externalId,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )

    private fun serving(
        id: String,
        foodId: String,
        label: String,
        grams: Double = 100.0,
        position: Int = 0,
    ) = FoodServing(
        id = id,
        userId = userId,
        foodId = foodId,
        label = label,
        grams = grams,
        position = position,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )
}

private class FakeFoodRemote(
    private val rows: MutableList<Food> = mutableListOf(),
) : FoodRemote {
    override suspend fun listForUser(userId: String): List<Food> = rows.filter { it.userId == userId }

    override suspend fun getById(id: String): Food? = rows.firstOrNull { it.id == id }

    override suspend fun findByExternal(
        userId: String,
        source: String,
        externalId: String,
    ): Food? =
        rows.firstOrNull {
            it.userId == userId && it.externalSource == source && it.externalId == externalId
        }

    override suspend fun insert(food: Food): Food {
        val saved = food.copy(id = "generated-${rows.size}")
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

private class FakeFoodServingRemote(
    private val rows: MutableList<FoodServing> = mutableListOf(),
) : FoodServingRemote {
    override suspend fun listForFood(foodId: String): List<FoodServing> =
        rows.filter { it.foodId == foodId }

    override suspend fun insert(serving: FoodServing): FoodServing {
        val saved = serving.copy(id = "generated-${rows.size}")
        rows += saved
        return saved
    }

    override suspend fun update(serving: FoodServing): FoodServing {
        val idx = rows.indexOfFirst { it.id == serving.id }
        require(idx >= 0)
        rows[idx] = serving
        return serving
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }
}
