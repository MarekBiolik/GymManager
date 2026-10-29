package com.gymmanager.core.sync

sealed interface ReadResult<out T> {
    data class Fresh<T>(val data: T) : ReadResult<T>

    data class Stale<T>(
        val data: T,
        val fetchedAtEpochMs: Long,
    ) : ReadResult<T>

    data class Empty(
        val reason: EmptyReason,
    ) : ReadResult<Nothing>
}

enum class EmptyReason {
    OFFLINE_MISS,
    ERROR_MISS,
    NOT_FOUND,
}

sealed interface WriteResult<out T> {
    data class Success<T>(val data: T) : WriteResult<T>

    data object OfflineRejected : WriteResult<Nothing>

    data class Failed(
        val message: String,
    ) : WriteResult<Nothing>
}
