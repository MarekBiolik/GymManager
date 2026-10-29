package com.gymmanager.android.auth

import com.gymmanager.android.BuildConfig

/**
 * Supabase + Google OAuth values from [BuildConfig] (sourced from gitignored local.properties).
 * Never put service_role or Google Client Secret in the app.
 */
data class AndroidAuthConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val googleWebClientId: String,
) {
    val isConfigured: Boolean
        get() =
            supabaseUrl.isNotBlank() &&
                supabaseAnonKey.isNotBlank() &&
                googleWebClientId.isNotBlank()

    companion object {
        fun fromBuildConfig(): AndroidAuthConfig =
            AndroidAuthConfig(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
                googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            )
    }
}
