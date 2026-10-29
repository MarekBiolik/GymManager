package com.gymmanager.core.data.supabase

import com.gymmanager.core.domain.Measurement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MeasurementDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val metric: String,
    val value: Double,
    @SerialName("measured_at") val measuredAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

fun MeasurementDto.toDomain(): Measurement =
    Measurement(
        id = id,
        userId = userId,
        metric = metric,
        value = value,
        measuredAtEpochMs = parseTimestamptzToEpochMs(measuredAt),
        createdAtEpochMs = parseTimestamptzToEpochMs(createdAt),
        updatedAtEpochMs = parseTimestamptzToEpochMs(updatedAt),
    )

fun Measurement.toDto(): MeasurementDto =
    MeasurementDto(
        id = id,
        userId = userId,
        metric = metric,
        value = value,
        measuredAt = epochMsToTimestamptz(measuredAtEpochMs),
        createdAt = epochMsToTimestamptz(createdAtEpochMs),
        updatedAt = epochMsToTimestamptz(updatedAtEpochMs),
    )
