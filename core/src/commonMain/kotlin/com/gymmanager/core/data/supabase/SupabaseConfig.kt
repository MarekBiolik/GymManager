package com.gymmanager.core.data.supabase

/**
 * Platform-injected Supabase client config. Never put service_role here.
 */
data class SupabaseConfig(
    val projectUrl: String,
    val anonKey: String,
) {
    init {
        require(projectUrl.isNotBlank()) { "Supabase projectUrl must not be blank" }
        require(anonKey.isNotBlank()) { "Supabase anonKey must not be blank" }
        require(!anonKey.contains("service_role", ignoreCase = true)) {
            "service_role must never be used in the client"
        }
    }
}
