package com.gymmanager.core.data.supabase

import com.gymmanager.core.application.profiles.ProfileRemote
import com.gymmanager.core.domain.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SupabaseProfileRemote(
    private val client: SupabaseClient,
) : ProfileRemote {
    override suspend fun fetchById(userId: String): Profile? {
        val row =
            client
                .from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }.decodeSingleOrNull<ProfileDto>()
        return row?.toDomain()
    }

    override suspend fun update(profile: Profile): Profile {
        val row =
            client
                .from("profiles")
                .update(
                    {
                        set("preferred_units", profile.preferredUnits.toDb())
                    },
                ) {
                    filter {
                        eq("id", profile.id)
                    }
                    select()
                }.decodeSingle<ProfileDto>()
        return row.toDomain()
    }
}
