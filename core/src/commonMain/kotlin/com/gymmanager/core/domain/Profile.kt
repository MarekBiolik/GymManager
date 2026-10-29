package com.gymmanager.core.domain

/**
 * App profile 1:1 with auth user. Minimum PII — no email/name/avatar.
 */
data class Profile(
    val id: String,
    val preferredUnits: PreferredUnits,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

enum class PreferredUnits {
    KG,
    LB,
    ;

    fun toDb(): String =
        when (this) {
            KG -> "kg"
            LB -> "lb"
        }

    companion object {
        fun fromDb(value: String): PreferredUnits =
            when (value) {
                "kg" -> KG
                "lb" -> LB
                else -> error("Unknown preferred_units: $value")
            }
    }
}
