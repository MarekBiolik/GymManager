package com.gymmanager.core.data.supabase

import com.gymmanager.core.application.measurements.MeasurementRemote
import com.gymmanager.core.domain.Measurement
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SupabaseMeasurementRemote(
    private val client: SupabaseClient,
) : MeasurementRemote {
    override suspend fun listForUser(userId: String): List<Measurement> =
        client
            .from("measurements")
            .select {
                filter {
                    eq("user_id", userId)
                }
                order("measured_at", Order.DESCENDING)
            }.decodeList<MeasurementDto>()
            .map { it.toDomain() }

    override suspend fun getById(id: String): Measurement? =
        client
            .from("measurements")
            .select {
                filter {
                    eq("id", id)
                }
            }.decodeSingleOrNull<MeasurementDto>()
            ?.toDomain()

    override suspend fun insert(measurement: Measurement): Measurement {
        val row =
            client
                .from("measurements")
                .insert(measurement.toDto()) {
                    select()
                }.decodeSingle<MeasurementDto>()
        return row.toDomain()
    }

    override suspend fun update(measurement: Measurement): Measurement {
        val row =
            client
                .from("measurements")
                .update(measurement.toDto()) {
                    filter {
                        eq("id", measurement.id)
                    }
                    select()
                }.decodeSingle<MeasurementDto>()
        return row.toDomain()
    }

    override suspend fun delete(id: String) {
        client.from("measurements").delete {
            filter {
                eq("id", id)
            }
        }
    }
}
