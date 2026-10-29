package com.gymmanager.core.sync

import com.gymmanager.core.domain.Measurement
import kotlin.test.Test
import kotlin.test.assertEquals

class LwwTest {
    @Test
    fun prefersLaterUpdatedAt() {
        val older =
            Measurement(
                id = "1",
                userId = "u",
                metric = "weight",
                value = 70.0,
                measuredAtEpochMs = 1,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 10,
            )
        val newer = older.copy(value = 71.0, updatedAtEpochMs = 20)
        assertEquals(71.0, lwwByUpdatedAt(older, newer) { it.updatedAtEpochMs }.value)
        assertEquals(71.0, lwwByUpdatedAt(newer, older) { it.updatedAtEpochMs }.value)
    }

    @Test
    fun equalUpdatedAtPrefersIncoming() {
        val a =
            Measurement(
                id = "1",
                userId = "u",
                metric = "weight",
                value = 1.0,
                measuredAtEpochMs = 1,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 5,
            )
        val b = a.copy(value = 2.0)
        assertEquals(2.0, lwwByUpdatedAt(a, b) { it.updatedAtEpochMs }.value)
    }
}
