package com.gymmanager.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymmanager.android.auth.AndroidAuthConfig
import com.gymmanager.android.auth.AuthViewModel
import com.gymmanager.android.auth.AuthViewModelFactory
import com.gymmanager.android.auth.GoogleSignInHelper
import com.gymmanager.android.sync.AndroidNetworkStatus
import com.gymmanager.android.sync.HelloSyncViewModel
import com.gymmanager.android.sync.HelloSyncViewModelFactory
import com.gymmanager.android.theme.GymManagerTheme
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.android.ui.AccountSettingsSection
import com.gymmanager.android.ui.AuthScreen
import com.gymmanager.android.ui.HelloSyncScreen
import com.gymmanager.core.GymManagerServices
import com.gymmanager.core.createGymManagerServices
import com.gymmanager.core.data.supabase.SupabaseConfig
import com.gymmanager.core.domain.auth.AuthState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authConfig = AndroidAuthConfig.fromBuildConfig()
        val network = AndroidNetworkStatus(applicationContext)

        val services: GymManagerServices? =
            if (authConfig.isConfigured) {
                createGymManagerServices(
                    config =
                        SupabaseConfig(
                            projectUrl = authConfig.supabaseUrl,
                            anonKey = authConfig.supabaseAnonKey,
                        ),
                    scope = lifecycleScope,
                    network = network,
                )
            } else {
                null
            }

        val googleSignInHelper = GoogleSignInHelper(applicationContext)
        val viewModelFactory =
            AuthViewModelFactory(
                authSession = services?.auth,
                googleSignInHelper = googleSignInHelper,
                config = authConfig,
            )

        setContent {
            GymManagerTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(GymTokens.ColorBgCanvas),
                ) {
                    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
                    val authUi by authViewModel.uiState.collectAsStateWithLifecycle()
                    AuthScreen(
                        viewModel = authViewModel,
                        signedInExtra = {
                            val signedIn = authUi.authState as? AuthState.SignedIn
                            if (signedIn != null && services != null) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val helloVm: HelloSyncViewModel =
                                        viewModel(
                                            key = signedIn.userId,
                                            factory =
                                                HelloSyncViewModelFactory(
                                                    userId = signedIn.userId,
                                                    profiles = services.profiles,
                                                    measurements = services.measurements,
                                                    network = network,
                                                ),
                                        )
                                    HelloSyncScreen(viewModel = helloVm)
                                    AccountSettingsSection(
                                        deleteAccount = services.deleteAccount,
                                        onDeleted = {
                                            services.profiles.onLogout(signedIn.userId)
                                            services.measurements.onLogout(signedIn.userId)
                                            authViewModel.signOut()
                                        },
                                    )
                                }
                            }
                        },
                        onBeforeSignOut = {
                            val signedIn = authUi.authState as? AuthState.SignedIn
                            if (signedIn != null && services != null) {
                                services.profiles.onLogout(signedIn.userId)
                                services.measurements.onLogout(signedIn.userId)
                            }
                        },
                    )
                }
            }
        }
    }
}
