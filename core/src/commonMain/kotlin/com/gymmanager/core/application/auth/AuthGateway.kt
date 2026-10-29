package com.gymmanager.core.application.auth

import com.gymmanager.core.domain.auth.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Port for Supabase Auth (or a fake in unit tests).
 * Implementations must never use service_role.
 */
interface AuthGateway {
    val authState: StateFlow<AuthState>

    suspend fun signInWithGoogleIdToken(idToken: String, nonce: String?)

    suspend fun signOut()
}
