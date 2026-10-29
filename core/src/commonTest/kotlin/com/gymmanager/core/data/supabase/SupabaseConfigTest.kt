package com.gymmanager.core.data.supabase

import kotlin.test.Test
import kotlin.test.assertFailsWith

class SupabaseConfigTest {
    @Test
    fun rejectsServiceRoleKey() {
        assertFailsWith<IllegalArgumentException> {
            SupabaseConfig(
                projectUrl = "https://example.supabase.co",
                anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.service_role.fake",
            )
        }
    }

    @Test
    fun acceptsAnonPlaceholder() {
        SupabaseConfig(
            projectUrl = "https://example.supabase.co",
            anonKey = "your-anon-key",
        )
    }
}
