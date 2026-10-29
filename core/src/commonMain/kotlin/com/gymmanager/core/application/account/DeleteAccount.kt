package com.gymmanager.core.application.account

/**
 * Invokes the trusted delete-account Edge Function with the current user JWT.
 * Implementations must never embed service_role.
 */
fun interface DeleteAccount {
    suspend fun invoke()
}
