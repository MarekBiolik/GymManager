package com.gymmanager.core.data.supabase

import com.gymmanager.core.application.auth.AuthGateway
import com.gymmanager.core.domain.auth.AuthState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * AuthGateway backed by supabase-kt. Uses user JWT from Auth; never service_role.
 */
class SupabaseAuthGateway(
    private val client: SupabaseClient,
    scope: CoroutineScope,
) : AuthGateway {
    override val authState: StateFlow<AuthState> =
        client.auth.sessionStatus
            .map { status -> status.toAuthState() }
            .stateIn(scope, SharingStarted.Eagerly, AuthState.SignedOut)

    override suspend fun signInWithGoogleIdToken(idToken: String, nonce: String?) {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            this.nonce = nonce
        }
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    private fun SessionStatus.toAuthState(): AuthState =
        when (this) {
            is SessionStatus.Authenticated -> {
                val session = this.session
                val userId = session.user?.id
                if (userId.isNullOrBlank() || session.accessToken.isBlank()) {
                    AuthState.SignedOut
                } else {
                    AuthState.SignedIn(
                        userId = userId,
                        accessToken = session.accessToken,
                    )
                }
            }
            else -> AuthState.SignedOut
        }
}
