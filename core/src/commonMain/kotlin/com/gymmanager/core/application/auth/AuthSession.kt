package com.gymmanager.core.application.auth

import com.gymmanager.core.domain.auth.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Public auth facade for Android/iOS: exchange Google id_token, observe session, sign out.
 * Platforms obtain the Google id_token; this API exchanges it for a Supabase JWT session.
 */
interface AuthSession {
    val state: StateFlow<AuthState>

    /**
     * Completes native Google sign-in by exchanging [idToken] for a Supabase session.
     * Optional [nonce] must match the nonce used when requesting the Google token (PKCE/nonce flows).
     */
    suspend fun completeGoogleSignIn(idToken: String, nonce: String? = null)

    suspend fun signOut()

    /** Current Supabase access JWT for protected calls, or null when signed out. */
    fun accessTokenOrNull(): String?
}
