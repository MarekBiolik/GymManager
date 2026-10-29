package com.gymmanager.core.application.measurements

import com.gymmanager.core.domain.Measurement

interface MeasurementRemote {
    suspend fun listForUser(userId: String): List<Measurement>

    suspend fun getById(id: String): Measurement?

    suspend fun insert(measurement: Measurement): Measurement

    suspend fun update(measurement: Measurement): Measurement

    suspend fun delete(id: String)
}
