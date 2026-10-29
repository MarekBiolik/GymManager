package com.gymmanager.core.application.auth

import com.gymmanager.core.domain.auth.AuthState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class DefaultAuthSessionTest {
    @Test
    fun sessionTransitionsSignedOutToSignedInToSignedOut() = runTest {
        val gateway = FakeAuthGateway()
        val session = createAuthSession(gateway)

        assertIs<AuthState.SignedOut>(session.state.value)
        assertNull(session.accessTokenOrNull())

        session.completeGoogleSignIn(idToken = "google-id-token", nonce = "n1")

        val signedIn = assertIs<AuthState.SignedIn>(session.state.value)
        assertEquals("user-from-token", signedIn.userId)
        assertEquals("jwt-google-id-token", signedIn.accessToken)
        assertEquals("jwt-google-id-token", session.accessTokenOrNull())
        assertEquals("google-id-token", gateway.lastIdToken)
        assertEquals("n1", gateway.lastNonce)

        session.signOut()

        assertIs<AuthState.SignedOut>(session.state.value)
        assertNull(session.accessTokenOrNull())
        assertEquals(1, gateway.signOutCount)
    }

    @Test
    fun blankIdTokenIsRejected() = runTest {
        val session = createAuthSession(FakeAuthGateway())
        assertFailsWith<IllegalArgumentException> {
            session.completeGoogleSignIn("  ")
        }
        assertIs<AuthState.SignedOut>(session.state.value)
    }
}
