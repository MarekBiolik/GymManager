package com.gymmanager.core.data.supabase

import com.gymmanager.core.domain.PreferredUnits
import com.gymmanager.core.domain.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("preferred_units") val preferredUnits: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

fun ProfileDto.toDomain(): Profile =
    Profile(
        id = id,
        preferredUnits = PreferredUnits.fromDb(preferredUnits),
        createdAtEpochMs = parseTimestamptzToEpochMs(createdAt),
        updatedAtEpochMs = parseTimestamptzToEpochMs(updatedAt),
    )

fun Profile.toDto(): ProfileDto =
    ProfileDto(
        id = id,
        preferredUnits = preferredUnits.toDb(),
        createdAt = epochMsToTimestamptz(createdAtEpochMs),
        updatedAt = epochMsToTimestamptz(updatedAtEpochMs),
    )
