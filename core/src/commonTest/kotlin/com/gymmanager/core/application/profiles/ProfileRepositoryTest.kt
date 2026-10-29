package com.gymmanager.core.application.profiles

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.domain.PreferredUnits
import com.gymmanager.core.domain.Profile
import com.gymmanager.core.sync.CacheKeys
import com.gymmanager.core.sync.InMemoryReadCache
import com.gymmanager.core.sync.ReadResult
import com.gymmanager.core.sync.WriteResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ProfileRepositoryTest {
    private val userId = "user-a"

    @Test
    fun logoutWipesProfileCache() =
        runTest {
            val cache = InMemoryReadCache()
            val remote = FakeProfileRemote(profile(userId))
            val repo =
                ProfileRepository(
                    remote = remote,
                    cache = cache,
                    network = NetworkStatus { true },
                    nowEpochMs = { 10L },
                )
            assertIs<ReadResult.Fresh<*>>(repo.readCurrent(userId))
            repo.onLogout(userId)
            assertNull(cache.get<Profile>(CacheKeys.profile(userId)))
        }

    @Test
    fun offlineUpdateRejected() =
        runTest {
            val repo =
                ProfileRepository(
                    remote = FakeProfileRemote(profile(userId)),
                    cache = InMemoryReadCache(),
                    network = NetworkStatus { false },
                    nowEpochMs = { 10L },
                )
            assertIs<WriteResult.OfflineRejected>(
                repo.updatePreferredUnits(userId, PreferredUnits.LB),
            )
        }

    private fun profile(id: String) =
        Profile(
            id = id,
            preferredUnits = PreferredUnits.KG,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 1,
        )
}

private class FakeProfileRemote(
    private var profile: Profile?,
) : ProfileRemote {
    override suspend fun fetchById(userId: String): Profile? = profile?.takeIf { it.id == userId }

    override suspend fun update(profile: Profile): Profile {
        this.profile = profile
        return profile
    }
}
