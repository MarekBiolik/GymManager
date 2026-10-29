package com.gymmanager.android.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Obtains a Google ID token via Credential Manager for exchange with Supabase (core [AuthSession]).
 */
class GoogleSignInHelper(
    private val context: Context,
) {
    suspend fun requestIdToken(serverClientId: String): Result<String> {
        if (serverClientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Web Client ID is not configured"))
        }

        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

        return try {
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val token = googleCredential.idToken
                if (token.isNullOrBlank()) {
                    Result.failure(IllegalStateException("Google returned an empty id_token"))
                } else {
                    Result.success(token)
                }
            } else {
                Result.failure(IllegalStateException("Unexpected credential type from Google Sign-In"))
            }
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(e)
        }
    }
}
