package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.MealType
import com.gymmanager.core.domain.NutritionSettings
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

class NutritionSettingsRepositoryTest {
    private val userId = "user-a"

    @Test
    fun offlineReadServesStaleCache() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeNutritionSettingsRemote(settings(userId))
            val online =
                NutritionSettingsRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 100L },
                )
            assertIs<ReadResult.Fresh<*>>(online.read(userId))

            val offline =
                NutritionSettingsRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { false },
                    nowEpochMs = { 200L },
                )
            val stale = assertIs<ReadResult.Stale<NutritionSettings>>(offline.read(userId))
            assertEquals(150.0, stale.data.proteinTargetG)
            assertEquals(100L, stale.fetchedAtEpochMs)
        }

    @Test
    fun offlineUpsertRejected() =
        runTest {
            val repo =
                NutritionSettingsRepository(
                    remote = FakeNutritionSettingsRemote(settings(userId)),
                    cache = InMemoryReadCache(),
                    network = NetworkStatus { false },
                    nowEpochMs = { 10L },
                )
            assertIs<WriteResult.OfflineRejected>(repo.upsert(settings(userId)))
        }

    @Test
    fun upsertCachesFreshRow() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeNutritionSettingsRemote(settings(userId))
            val repo =
                NutritionSettingsRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 50L },
                )
            val result =
                assertIs<WriteResult.Success<NutritionSettings>>(
                    repo.upsert(settings(userId).copy(proteinTargetG = 180.0)),
                )
            assertEquals(180.0, result.data.proteinTargetG)
            assertEquals(50L, result.data.updatedAtEpochMs)
            assertEquals(
                180.0,
                cache.get<NutritionSettings>(CacheKeys.nutritionSettings(userId))!!.payload.proteinTargetG,
            )
        }

    @Test
    fun userMismatchIsCacheMiss() =
        runTest {
            val cache = InMemoryReadCache()
            cache.put(
                CacheKeys.nutritionSettings(userId),
                com.gymmanager.core.sync.CacheEntry(
                    payload = settings("other"),
                    fetchedAtEpochMs = 1,
                    userId = "other",
                ),
            )
            val repo =
                NutritionSettingsRepository(
                    remote = FakeNutritionSettingsRemote(null),
                    cache = cache,
                    network = NetworkStatus { false },
                    nowEpochMs = { 200L },
                )
            val empty = assertIs<ReadResult.Empty>(repo.read(userId))
            assertEquals(EmptyReason.OFFLINE_MISS, empty.reason)
            assertNull(cache.get<NutritionSettings>(CacheKeys.nutritionSettings(userId)))
        }

    private fun settings(id: String) =
        NutritionSettings(
            userId = id,
            calorieTargetKcal = 2000.0,
            proteinTargetG = 150.0,
            carbsTargetG = 200.0,
            fatTargetG = 65.0,
            heightCm = 180.0,
            waterTargetMl = 2500.0,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )
}

private class FakeNutritionSettingsRemote(
    private var row: NutritionSettings?,
) : NutritionSettingsRemote {
    override suspend fun fetch(userId: String): NutritionSettings? =
        row?.takeIf { it.userId == userId }

    override suspend fun upsert(settings: NutritionSettings): NutritionSettings {
        row = settings
        return settings
    }
}

class MealTypeRepositoryTest {
    private val userId = "user-a"

    @Test
    fun listSortsByPositionAndCaches() =
        runTest {
            val cache = InMemoryReadCache()
            val remote =
                FakeMealTypeRemote(
                    mutableListOf(
                        meal("m2", "Lunch", 2),
                        meal("m0", "Breakfast", 0),
                        meal("m1", "Snack", 1),
                    ),
                )
            val repo =
                MealTypeRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 10L },
                )
            val fresh = assertIs<ReadResult.Fresh<List<MealType>>>(repo.list(userId))
            assertEquals(listOf("Breakfast", "Snack", "Lunch"), fresh.data.map { it.name })
            assertEquals(
                3,
                cache.get<List<MealType>>(CacheKeys.mealTypesList(userId))!!.payload.size,
            )
        }

    @Test
    fun offlineInsertRejected() =
        runTest {
            val repo =
                MealTypeRepository(
                    remote = FakeMealTypeRemote(),
                    cache = InMemoryReadCache(),
                    network = NetworkStatus { false },
                    nowEpochMs = { 10L },
                )
            assertIs<WriteResult.OfflineRejected>(repo.insert(userId, "Brunch", 5))
        }

    @Test
    fun insertInvalidatesListCache() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeMealTypeRemote(mutableListOf(meal("m0", "Breakfast", 0)))
            val repo =
                MealTypeRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 20L },
                )
            assertIs<ReadResult.Fresh<*>>(repo.list(userId))
            assertIs<WriteResult.Success<*>>(repo.insert(userId, "Brunch", 1))
            assertNull(cache.get<List<MealType>>(CacheKeys.mealTypesList(userId)))
        }

    private fun meal(
        id: String,
        name: String,
        position: Int,
    ) = MealType(
        id = id,
        userId = userId,
        name = name,
        position = position,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )
}

private class FakeMealTypeRemote(
    private val rows: MutableList<MealType> = mutableListOf(),
) : MealTypeRemote {
    override suspend fun listForUser(userId: String): List<MealType> =
        rows.filter { it.userId == userId }

    override suspend fun insert(mealType: MealType): MealType {
        val saved = mealType.copy(id = "generated-${rows.size}")
        rows += saved
        return saved
    }

    override suspend fun update(mealType: MealType): MealType {
        val idx = rows.indexOfFirst { it.id == mealType.id }
        require(idx >= 0)
        rows[idx] = mealType
        return mealType
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }
}
