package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.WaterEntry
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

class WaterRepositoryTest {
    private val userId = "user-a"
    private val day = "2026-10-30"

    @Test
    fun addListAndTotal() =
        runTest {
            val remote = FakeWaterEntryRemote()
            val cache = InMemoryReadCache()
            val repo =
                WaterRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 50L },
                )
            val saved =
                assertIs<WriteResult.Success<WaterEntry>>(
                    repo.add(userId, day, 250.0),
                )
            assertEquals(250.0, saved.data.amountMl)
            assertNull(cache.get<List<WaterEntry>>(CacheKeys.waterDay(userId, day)))

            val fresh = assertIs<ReadResult.Fresh<List<WaterEntry>>>(repo.listDay(userId, day))
            assertEquals(1, fresh.data.size)
            assertEquals(250.0, repo.totalMl(fresh.data))
        }

    @Test
    fun offlineListServesStaleCache() =
        runTest {
            val remote =
                FakeWaterEntryRemote(
                    mutableListOf(
                        WaterEntry("w1", userId, day, 100.0, 1, 1),
                    ),
                )
            val cache = InMemoryReadCache()
            val online =
                WaterRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 10L },
                )
            assertIs<ReadResult.Fresh<*>>(online.listDay(userId, day))

            val offline =
                WaterRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { false },
                    nowEpochMs = { 20L },
                )
            val stale = assertIs<ReadResult.Stale<List<WaterEntry>>>(offline.listDay(userId, day))
            assertEquals(100.0, stale.data.single().amountMl)
            assertEquals(10L, stale.fetchedAtEpochMs)
        }

    @Test
    fun offlineAddRejected() =
        runTest {
            val repo =
                WaterRepository(
                    remote = FakeWaterEntryRemote(),
                    cache = InMemoryReadCache(),
                    network = NetworkStatus { false },
                    nowEpochMs = { 1L },
                )
            assertIs<WriteResult.OfflineRejected>(repo.add(userId, day, 200.0))
        }

    @Test
    fun deleteInvalidatesCache() =
        runTest {
            val remote =
                FakeWaterEntryRemote(
                    mutableListOf(WaterEntry("w1", userId, day, 100.0, 1, 1)),
                )
            val cache = InMemoryReadCache()
            val repo =
                WaterRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 5L },
                )
            assertIs<ReadResult.Fresh<*>>(repo.listDay(userId, day))
            assertIs<WriteResult.Success<*>>(repo.delete(userId, day, "w1"))
            assertNull(cache.get<List<WaterEntry>>(CacheKeys.waterDay(userId, day)))
            val empty = assertIs<ReadResult.Fresh<List<WaterEntry>>>(repo.listDay(userId, day))
            assertEquals(0, empty.data.size)
        }

    @Test
    fun offlineMissWhenNoCache() =
        runTest {
            val repo =
                WaterRepository(
                    remote = FakeWaterEntryRemote(),
                    cache = InMemoryReadCache(),
                    network = NetworkStatus { false },
                    nowEpochMs = { 1L },
                )
            val empty = assertIs<ReadResult.Empty>(repo.listDay(userId, day))
            assertEquals(EmptyReason.OFFLINE_MISS, empty.reason)
        }
}

private class FakeWaterEntryRemote(
    private val rows: MutableList<WaterEntry> = mutableListOf(),
) : WaterEntryRemote {
    override suspend fun listForDay(
        userId: String,
        drunkOn: String,
    ): List<WaterEntry> = rows.filter { it.userId == userId && it.drunkOn == drunkOn }

    override suspend fun insert(entry: WaterEntry): WaterEntry {
        val saved = entry.copy(id = "w-${rows.size}")
        rows += saved
        return saved
    }

    override suspend fun delete(id: String) {
        rows.removeAll { it.id == id }
    }
}
