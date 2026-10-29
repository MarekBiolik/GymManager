package com.gymmanager.core.application

/** Platform-injected online check for online-first sync policy. */
fun interface NetworkStatus {
    fun isOnline(): Boolean
}
