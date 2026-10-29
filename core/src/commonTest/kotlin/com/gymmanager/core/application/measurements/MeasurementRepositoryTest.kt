package com.gymmanager.core.application.measurements

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.Measurement
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

class MeasurementRepositoryTest {
    private val userId = "user-a"

    @Test
    fun offlineWriteIsRejectedWithoutOptimisticCache() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeMeasurementRemote()
            val repo =
                MeasurementRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { false },
                    nowEpochMs = { 100L },
                )
            val result =
                repo.insert(
                    Measurement(
                        id = "m1",
                        userId = userId,
                        metric = "weight",
                        value = 80.0,
                        measuredAtEpochMs = 50,
                        createdAtEpochMs = 50,
                        updatedAtEpochMs = 50,
                    ),
                )
            assertIs<WriteResult.OfflineRejected>(result)
            assertNull(cache.get<Measurement>(CacheKeys.measurementItem("m1")))
            assertEquals(0, remote.insertCount)
        }

    @Test
    fun offlineReadServesStaleCache() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeMeasurementRemote()
            val onlineRepo =
                MeasurementRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 100L },
                )
            remote.list = listOf(sample("m1", 70.0))
            assertIs<ReadResult.Fresh<*>>(onlineRepo.list(userId))

            val offlineRepo =
                MeasurementRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { false },
                    nowEpochMs = { 200L },
                )
            val stale = assertIs<ReadResult.Stale<List<Measurement>>>(offlineRepo.list(userId))
            assertEquals(1, stale.data.size)
            assertEquals(100L, stale.fetchedAtEpochMs)
        }

    @Test
    fun userMismatchIsCacheMiss() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeMeasurementRemote()
            remote.list = listOf(sample("m1", 70.0))
            val repo =
                MeasurementRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 100L },
                )
            repo.list(userId)
            // Simulate leftover key for same logical slot but wrong user metadata by clearing and putting wrong user
            cache.clearAll()
            cache.put(
                CacheKeys.measurementsList(userId),
                com.gymmanager.core.sync.CacheEntry(
                    payload = listOf(sample("stolen", 1.0).copy(userId = "other")),
                    fetchedAtEpochMs = 1,
                    userId = "other",
                ),
            )
            val offline =
                MeasurementRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { false },
                    nowEpochMs = { 200L },
                )
            val empty = assertIs<ReadResult.Empty>(offline.list(userId))
            assertEquals(EmptyReason.OFFLINE_MISS, empty.reason)
        }

    @Test
    fun logoutWipesUserCache() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeMeasurementRemote(list = listOf(sample("m1", 70.0)))
            val repo =
                MeasurementRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 100L },
                )
            repo.list(userId)
            repo.onLogout(userId)
            assertNull(cache.get<List<Measurement>>(CacheKeys.measurementsList(userId)))
            assertNull(cache.get<Measurement>(CacheKeys.measurementItem("m1")))
        }

    @Test
    fun successfulInsertInvalidatesAndUpsertsCache() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeMeasurementRemote()
            val repo =
                MeasurementRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 100L },
                )
            val inserted = sample("m2", 81.0)
            val result = repo.insert(inserted)
            assertIs<WriteResult.Success<*>>(result)
            assertEquals(inserted, cache.get<Measurement>(CacheKeys.measurementItem("m2"))?.payload)
            assertEquals(listOf(inserted), cache.get<List<Measurement>>(CacheKeys.measurementsList(userId))?.payload)
        }

    private fun sample(id: String, value: Double) =
        Measurement(
            id = id,
            userId = userId,
            metric = "weight",
            value = value,
            measuredAtEpochMs = 50,
            createdAtEpochMs = 50,
            updatedAtEpochMs = 50,
        )
}

private class FakeMeasurementRemote(
    var list: List<Measurement> = emptyList(),
) : MeasurementRemote {
    var insertCount: Int = 0
        private set

    override suspend fun listForUser(userId: String): List<Measurement> = list.filter { it.userId == userId }

    override suspend fun getById(id: String): Measurement? = list.find { it.id == id }

    override suspend fun insert(measurement: Measurement): Measurement {
        insertCount += 1
        list = list + measurement
        return measurement
    }

    override suspend fun update(measurement: Measurement): Measurement {
        list = list.filterNot { it.id == measurement.id } + measurement
        return measurement
    }

    override suspend fun delete(id: String) {
        list = list.filterNot { it.id == id }
    }
}
