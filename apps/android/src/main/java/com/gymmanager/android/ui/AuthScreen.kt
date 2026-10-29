package com.gymmanager.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymmanager.android.auth.AuthUiState
import com.gymmanager.android.auth.AuthViewModel
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.domain.auth.AuthState

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    signedInExtra: @Composable () -> Unit = {},
    onBeforeSignOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AuthScreenContent(
        uiState = uiState,
        onSignInWithGoogle = viewModel::signInWithGoogle,
        onSignOut = {
            onBeforeSignOut()
            viewModel.signOut()
        },
        signedInExtra = signedInExtra,
    )
}

@Composable
private fun AuthScreenContent(
    uiState: AuthUiState,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    signedInExtra: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(GymTokens.Space4),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            !uiState.configured -> UnconfiguredMessage()
            uiState.busy && uiState.authState is AuthState.SignedOut ->
                CircularProgressIndicator(color = GymTokens.ColorTextPrimary)
            uiState.authState is AuthState.SignedIn -> {
                val signedIn = uiState.authState as AuthState.SignedIn
                Text(
                    text = "Signed in as ${truncateUserId(signedIn.userId)}",
                    color = GymTokens.ColorTextPrimary,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onSignOut,
                    enabled = !uiState.busy,
                    modifier = Modifier.padding(top = GymTokens.Space4),
                ) {
                    Text("Sign out")
                }
                signedInExtra()
                Text(
                    text = "Live OAuth / RLS smoke needs IdP (#73) and configured local.properties.",
                    color = GymTokens.ColorTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = GymTokens.Space3),
                )
            }
            else -> {
                Text(
                    text = "GymManager",
                    color = GymTokens.ColorTextPrimary,
                    modifier = Modifier.padding(bottom = GymTokens.Space4),
                )
                Button(
                    onClick = onSignInWithGoogle,
                    enabled = !uiState.busy,
                ) {
                    Text("Sign in with Google")
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = GymTokens.ColorTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = GymTokens.Space4),
            )
        }
    }
}

@Composable
private fun UnconfiguredMessage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Auth not configured",
            color = GymTokens.ColorTextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text =
                "Set gymmanager.supabase.url, gymmanager.supabase.anonKey, and " +
                    "gymmanager.google.webClientId in local.properties (see local.properties.example).",
            color = GymTokens.ColorTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = GymTokens.Space3),
        )
    }
}

private fun truncateUserId(userId: String): String {
    if (userId.length <= 12) return userId
    return "${userId.take(8)}…${userId.takeLast(4)}"
}
