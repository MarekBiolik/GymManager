package com.gymmanager.core.domain

/**
 * Smoke measurement row (hello-sync). Hard delete — no tombstones in MVP.
 */
data class Measurement(
    val id: String,
    val userId: String,
    val metric: String,
    val value: Double,
    val measuredAtEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
