package com.gymmanager.core

import com.gymmanager.core.application.NetworkStatus
import com.gymmanager.core.application.account.DeleteAccount
import com.gymmanager.core.application.auth.AuthSession
import com.gymmanager.core.application.auth.DefaultAuthSession
import com.gymmanager.core.application.measurements.MeasurementRepository
import com.gymmanager.core.application.profiles.ProfileRepository
import com.gymmanager.core.data.supabase.SupabaseAuthGateway
import com.gymmanager.core.data.supabase.SupabaseConfig
import com.gymmanager.core.data.supabase.SupabaseDeleteAccount
import com.gymmanager.core.data.supabase.SupabaseMeasurementRemote
import com.gymmanager.core.data.supabase.SupabaseProfileRemote
import com.gymmanager.core.data.supabase.createGymManagerSupabaseClient
import com.gymmanager.core.sync.InMemoryReadCache
import com.gymmanager.core.sync.ReadCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock

/**
 * Shared mobile wiring: one Supabase client → auth + hello-sync repositories + delete-account.
 */
data class GymManagerServices(
    val auth: AuthSession,
    val profiles: ProfileRepository,
    val measurements: MeasurementRepository,
    val deleteAccount: DeleteAccount,
    val cache: ReadCache,
)

fun createGymManagerServices(
    config: SupabaseConfig,
    scope: CoroutineScope,
    network: NetworkStatus,
    cache: ReadCache = InMemoryReadCache(),
    nowEpochMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
): GymManagerServices {
    val client = createGymManagerSupabaseClient(config)
    return GymManagerServices(
        auth = DefaultAuthSession(SupabaseAuthGateway(client, scope)),
        profiles =
            ProfileRepository(
                remote = SupabaseProfileRemote(client),
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        measurements =
            MeasurementRepository(
                remote = SupabaseMeasurementRemote(client),
                cache = cache,
                network = network,
                nowEpochMs = nowEpochMs,
            ),
        deleteAccount = SupabaseDeleteAccount(client),
        cache = cache,
    )
}
