package com.gymmanager.core.application.profiles

import com.gymmanager.core.domain.Profile

interface ProfileRemote {
    suspend fun fetchById(userId: String): Profile?

    suspend fun update(profile: Profile): Profile
}
