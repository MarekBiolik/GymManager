package com.gymmanager.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.application.account.DeleteAccount
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsSection(
    deleteAccount: DeleteAccount,
    onDeleted: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.Idle) }
    var phrase by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = GymTokens.Space4),
        verticalArrangement = Arrangement.spacedBy(GymTokens.Space3),
    ) {
        Text(text = "Account settings", color = GymTokens.ColorTextPrimary)
        Text(
            text =
                "Deleting your account permanently removes your GymManager data and Auth identity. " +
                    "Signing in again creates a new empty account.",
            color = GymTokens.ColorTextSecondary,
        )
        when (step) {
            Step.Idle -> {
                Button(
                    onClick = {
                        step = Step.Confirm
                        phrase = ""
                        error = null
                    },
                ) {
                    Text("Delete account…")
                }
            }
            Step.Confirm -> {
                Text(
                    text = "This cannot be undone. Type DELETE to confirm.",
                    color = GymTokens.ColorTextSecondary,
                )
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text("Type DELETE") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (phrase.trim().uppercase() != "DELETE") {
                            error = "Type DELETE to confirm."
                            return@Button
                        }
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                deleteAccount.invoke()
                                onDeleted()
                            } catch (e: Exception) {
                                error = e.message ?: "Delete account failed"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                ) {
                    Text("Delete forever")
                }
                Button(
                    onClick = { step = Step.Idle },
                    enabled = !busy,
                ) {
                    Text("Cancel")
                }
            }
        }
        error?.let {
            Text(text = it, color = GymTokens.ColorTextSecondary)
        }
        Text(
            text = "Live smoke needs deployed delete-account function and IdP (#73).",
            color = GymTokens.ColorTextSecondary,
        )
    }
}

private enum class Step {
    Idle,
    Confirm,
}
