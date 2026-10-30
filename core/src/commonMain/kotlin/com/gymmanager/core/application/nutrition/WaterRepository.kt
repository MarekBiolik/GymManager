package com.gymmanager.core.application.nutrition

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.WaterEntry
import com.gymmanager.core.sync.CacheEntry
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult

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
