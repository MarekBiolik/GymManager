package com.gymmanager.core.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Builds a Supabase client with Auth + Postgrest + Functions using the public anon key only.
 */
fun createGymManagerSupabaseClient(config: SupabaseConfig): SupabaseClient =
    createSupabaseClient(
        supabaseUrl = config.projectUrl,
        supabaseKey = config.anonKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Functions)
    }
