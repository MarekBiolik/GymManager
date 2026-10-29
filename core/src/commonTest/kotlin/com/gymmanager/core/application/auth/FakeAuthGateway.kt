package com.gymmanager.core.application.auth

import com.gymmanager.core.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeAuthGateway(
    initial: AuthState = AuthState.SignedOut,
) : AuthGateway {
    private val _state = MutableStateFlow(initial)
    override val authState: StateFlow<AuthState> = _state.asStateFlow()

    var lastIdToken: String? = null
        private set
    var lastNonce: String? = null
        private set
    var signOutCount: Int = 0
        private set

    override suspend fun signInWithGoogleIdToken(idToken: String, nonce: String?) {
        lastIdToken = idToken
        lastNonce = nonce
        _state.value = AuthState.SignedIn(
            userId = "user-from-token",
            accessToken = "jwt-$idToken",
        )
    }

    override suspend fun signOut() {
        signOutCount += 1
        _state.value = AuthState.SignedOut
    }
}
