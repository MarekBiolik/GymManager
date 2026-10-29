package com.gymmanager.core.application.profiles

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.PreferredUnits
import com.gymmanager.core.domain.Profile
import com.gymmanager.core.sync.CacheEntry
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.EmptyReason
import com.gymmanager.core.sync.ReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import com.gymmanager.core.sync.lwwByUpdatedAt

class ProfileRepository(
    private val remote: ProfileRemote,
    private val cache: ReadCache,
    private val network: NetworkStatus,
    private val nowEpochMs: () -> Long,
) {
    suspend fun readCurrent(userId: String): ReadResult<Profile> {
        require(userId.isNotBlank())
        if (!network.isOnline()) {
            return staleOrEmpty(userId, EmptyReason.OFFLINE_MISS)
        }
        return try {
            val fresh = remote.fetchById(userId)
            if (fresh == null) {
                cache.remove(CacheKeys.profile(userId))
                ReadResult.Empty(EmptyReason.NOT_FOUND)
            } else {
                cache.put(
                    CacheKeys.profile(userId),
                    CacheEntry(fresh, nowEpochMs(), userId),
                )
                ReadResult.Fresh(fresh)
            }
        } catch (_: Exception) {
            staleOrEmpty(userId, EmptyReason.ERROR_MISS)
        }
    }

    suspend fun updatePreferredUnits(
        userId: String,
        units: PreferredUnits,
    ): WriteResult<Profile> {
        if (!network.isOnline()) return WriteResult.OfflineRejected
        val existing =
            when (val read = readCurrent(userId)) {
                is ReadResult.Fresh -> read.data
                is ReadResult.Stale -> read.data
                is ReadResult.Empty -> return WriteResult.Failed("Profile not found")
            }
        return try {
            val updated =
                remote.update(
                    existing.copy(
                        preferredUnits = units,
                        updatedAtEpochMs = nowEpochMs(),
                    ),
                )
            cache.put(
                CacheKeys.profile(userId),
                CacheEntry(updated, nowEpochMs(), userId),
            )
            WriteResult.Success(updated)
        } catch (e: Exception) {
            WriteResult.Failed(e.message ?: "Update failed")
        }
    }

    fun onLogout(previousUserId: String?) {
        if (previousUserId != null) {
            cache.clearForUser(previousUserId)
        } else {
            cache.clearAll()
        }
    }

    fun onUserChanged(previousUserId: String?, newUserId: String) {
        if (previousUserId != null && previousUserId != newUserId) {
            cache.clearForUser(previousUserId)
        }
    }

    private fun staleOrEmpty(userId: String, miss: EmptyReason): ReadResult<Profile> {
        val hit = cache.get<Profile>(CacheKeys.profile(userId))
        return if (hit != null && hit.userId == userId) {
            ReadResult.Stale(hit.payload, hit.fetchedAtEpochMs)
        } else {
            // User mismatch or miss → treat as miss (never leak another user's rows)
            if (hit != null && hit.userId != userId) {
                cache.remove(CacheKeys.profile(userId))
            }
            ReadResult.Empty(miss)
        }
    }

    fun mergeCached(userId: String, incoming: Profile): Profile {
        val cached = cache.get<Profile>(CacheKeys.profile(userId))?.payload
        return lwwByUpdatedAt(cached, incoming) { it.updatedAtEpochMs }
    }
}
