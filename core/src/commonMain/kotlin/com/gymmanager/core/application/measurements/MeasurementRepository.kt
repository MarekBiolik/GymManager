package com.gymmanager.core.application.measurements

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.Measurement
import com.gymmanager.core.sync.CacheEntry
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import com.gymmanager.core.sync.lwwByUpdatedAt

class MeasurementRepository(
    private val remote: MeasurementRemote,
    private val cache: ReadCache,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun list(userId: String): ReadResult<List<Measurement>> {
        require(userId.isNotBlank())
        if (!network.isOnline()) {
            return staleListOrEmpty(userId, EmptyReason.OFFLINE_MISS)
        }
        return try {
            val fresh = remote.listForUser(userId).sortedByDescending { it.measuredAtEpochMs }
            cache.put(
                CacheKeys.measurementsList(userId),
                CacheEntry(fresh, nowEpochMs(), userId),
            )
            fresh.forEach { m ->
                cache.put(
                    CacheKeys.measurementItem(m.id),
                    CacheEntry(m, nowEpochMs(), userId),
                )
            }
            ReadResult.Fresh(fresh)
        } catch (_: Exception) {
            staleListOrEmpty(userId, EmptyReason.ERROR_MISS)
        }
    }

    suspend fun insert(measurement: Measurement): WriteResult<Measurement> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val saved = remote.insert(measurement)
            upsertCaches(saved)
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Insert failed")
        }
    }

    suspend fun update(measurement: Measurement): WriteResult<Measurement> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            val saved = remote.update(measurement)
            upsertCaches(saved)
            WriteResult.Success(saved)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Update failed")
        }
    }

    suspend fun delete(userId: String, id: String): WriteResult<Unit> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        return try {
            remote.delete(id)
            cache.remove(CacheKeys.measurementItem(id))
            val listKey = CacheKeys.measurementsList(userId)
            val list = cache.get<List<Measurement>>(listKey)
            if (list != null && list.userId == userId) {
                cache.put(
                    listKey,
                    list.copy(payload = list.payload.filterNot { it.id == id }, fetchedAtEpochMs = nowEpochMs()),
                )
            }
            WriteResult.Success(Unit)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Delete failed")
        }
    }

    fun onLogout(previousUserId: String?) {
        if (previousUserId != null) {
            cache.clearForUser(previousUserId)
        } else {
            cache.clearAll()
        }
    }

    fun mergeItem(cached: Measurement?, incoming: Measurement): Measurement =
        lwwByUpdatedAt(cached, incoming) { it.updatedAtEpochMs }

    private fun upsertCaches(saved: Measurement) {
        val now = nowEpochMs()
        val previousItem = cache.get<Measurement>(CacheKeys.measurementItem(saved.id))?.payload
        val winner = mergeItem(previousItem, saved)
        cache.put(
            CacheKeys.measurementItem(saved.id),
            CacheEntry(winner, now, saved.userId),
        )
        val listKey = CacheKeys.measurementsList(saved.userId)
        val existing = cache.get<List<Measurement>>(listKey)
        val mergedList =
            if (existing != null && existing.userId == saved.userId) {
                val base = existing.payload.filterNot { it.id == saved.id }
                (base + winner).sortedByDescending { it.measuredAtEpochMs }
            } else {
                listOf(winner)
            }
        cache.put(listKey, CacheEntry(mergedList, now, saved.userId))
    }

    private fun staleListOrEmpty(userId: String, miss: EmptyReason): ReadResult<List<Measurement>> {
        val hit = cache.get<List<Measurement>>(CacheKeys.measurementsList(userId))
        return if (hit != null && hit.userId == userId) {
            ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
        } else {
            ReadResult.Empty(miss)
        }
    }
}
