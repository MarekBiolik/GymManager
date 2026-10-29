package com.gymmanager.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymmanager.core.application.auth.AuthSession
import com.gymmanager.core.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val configured: Boolean,
    val authState: AuthState,
    val busy: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    private val authSession: AuthSession?,
    private val googleSignInHelper: GoogleSignInHelper,
    private val googleWebClientId: String,
    configured: Boolean,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            AuthUiState(
                configured = configured,
                authState = authSession?.state?.value ?: AuthState.SignedOut,
            ),
        )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        authSession?.let { session ->
            viewModelScope.launch {
                session.state.collect { authState ->
                    _uiState.value =
                        _uiState.value.copy(authState = authState, busy = false)
                }
            }
        }
    }

    fun signInWithGoogle() {
        val session = authSession ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, errorMessage = null)
            googleSignInHelper.requestIdToken(googleWebClientId)
                .onSuccess { idToken ->
                    try {
                        session.completeGoogleSignIn(idToken)
                    } catch (e: Exception) {
                        _uiState.value =
                            _uiState.value.copy(
                                busy = false,
                                errorMessage = e.message ?: "Sign-in failed",
                            )
                    }
                }
                .onFailure { e ->
                    _uiState.value =
                        _uiState.value.copy(
                            busy = false,
                            errorMessage = e.message ?: "Google Sign-In failed",
                        )
                }
        }
    }

    fun signOut() {
        val session = authSession ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, errorMessage = null)
            try {
                session.signOut()
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        busy = false,
                        errorMessage = e.message ?: "Sign-out failed",
                    )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class AuthViewModelFactory(
    private val authSession: AuthSession?,
    private val googleSignInHelper: GoogleSignInHelper,
    private val config: AndroidAuthConfig,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(
                authSession = authSession,
                googleSignInHelper = googleSignInHelper,
                googleWebClientId = config.googleWebClientId,
                configured = config.isConfigured,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
