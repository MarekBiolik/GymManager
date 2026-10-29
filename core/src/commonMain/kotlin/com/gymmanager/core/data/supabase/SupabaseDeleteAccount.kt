package com.gymmanager.core.data.supabase

import com.gymmanager.core.application.account.DeleteAccount
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions

class SupabaseDeleteAccount(
    private val client: SupabaseClient,
) : DeleteAccount {
    override suspend fun invoke() {
        client.functions.invoke("delete-account")
    }
}
