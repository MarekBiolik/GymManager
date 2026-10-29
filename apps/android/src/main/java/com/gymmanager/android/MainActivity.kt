package com.gymmanager.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymmanager.android.auth.AndroidAuthConfig
import com.gymmanager.android.auth.AuthViewModel
import com.gymmanager.android.auth.AuthViewModelFactory
import com.gymmanager.android.auth.GoogleSignInHelper
import com.gymmanager.android.theme.GymManagerTheme
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.android.ui.AuthScreen
import com.gymmanager.core.application.auth.createAuthSession
import com.gymmanager.core.data.supabase.SupabaseConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authConfig = AndroidAuthConfig.fromBuildConfig()
        val authSession =
            if (authConfig.isConfigured) {
                createAuthSession(
                    SupabaseConfig(
                        projectUrl = authConfig.supabaseUrl,
                        anonKey = authConfig.supabaseAnonKey,
                    ),
                    lifecycleScope,
                )
            } else {
                null
            }
        val googleSignInHelper = GoogleSignInHelper(applicationContext)
        val viewModelFactory =
            AuthViewModelFactory(
                authSession = authSession,
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
                    val viewModel: AuthViewModel = viewModel(factory = viewModelFactory)
                    AuthScreen(viewModel = viewModel)
                }
            }
        }
    }
}
