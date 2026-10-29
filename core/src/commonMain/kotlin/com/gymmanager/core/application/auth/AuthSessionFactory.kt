package com.gymmanager.core.application.auth

import com.gymmanager.core.data.supabase.SupabaseAuthGateway
import com.gymmanager.core.data.supabase.SupabaseConfig
import com.gymmanager.core.data.supabase.createGymManagerSupabaseClient
import kotlinx.coroutines.CoroutineScope

/**
 * Creates an [AuthSession] wired to supabase-kt for the given [config].
 * [scope] drives session-status collection inside the Supabase gateway.
 */
fun createAuthSession(
    config: SupabaseConfig,
    scope: CoroutineScope,
): AuthSession {
    val client = createGymManagerSupabaseClient(config)
    return DefaultAuthSession(SupabaseAuthGateway(client, scope))
}

/**
 * Creates an [AuthSession] over an existing [gateway] (tests / custom wiring).
 */
fun createAuthSession(gateway: AuthGateway): AuthSession = DefaultAuthSession(gateway)
