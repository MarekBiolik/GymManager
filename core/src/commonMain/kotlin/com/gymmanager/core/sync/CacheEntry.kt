package com.gymmanager.core.sync

/**
 * Platform-independent cache entry (hello-sync contract).
 */
data class CacheEntry<T>(
    val payload: T,
    val fetchedAtEpochMs: Long,
    val userId: String,
)
