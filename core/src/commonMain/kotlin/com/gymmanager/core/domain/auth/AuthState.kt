package com.gymmanager.core.domain.auth

/**
 * Auth session state for clients. Minimum PII: user id only (no IdP email/name/avatar).
 */
sealed interface AuthState {
    data object SignedOut : AuthState

    data class SignedIn(
        val userId: String,
        val accessToken: String,
    ) : AuthState
}
