package com.gymmanager.core.sync

/**
 * Last-write-wins by updated_at epoch millis (ADR 0008).
 * Returns the row with the later [updatedAtEpochMs]; equal → [incoming].
 */
fun <T> lwwByUpdatedAt(
    cached: T?,
    incoming: T,
    updatedAt: (T) -> Long,
): T {
    if (cached == null) return incoming
    return if (updatedAt(incoming) >= updatedAt(cached)) incoming else cached
}
