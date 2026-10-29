package com.gymmanager.core.application.auth

import com.gymmanager.core.domain.auth.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin AuthSession facade over [AuthGateway].
 */
class DefaultAuthSession(
    private val gateway: AuthGateway,
) : AuthSession {
    override val state: StateFlow<AuthState> = gateway.authState

    override suspend fun completeGoogleSignIn(idToken: String, nonce: String?) {
        require(idToken.isNotBlank()) { "Google id_token must not be blank" }
        gateway.signInWithGoogleIdToken(idToken, nonce)
    }

    override suspend fun signOut() {
        gateway.signOut()
    }

    override fun accessTokenOrNull(): String? =
        (state.value as? AuthState.SignedIn)?.accessToken
}
