package com.anish.owee.data.repository

import com.anish.owee.data.model.CreateSettlementRequest
import com.anish.owee.data.model.Settlement
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID

class SettlementRepositoryImpl : SettlementRepository {

    private val client = SupabaseProvider.client

    private val postgrest = client.postgrest

    override suspend fun createSettlement(
        sourceType: String,
        sourceId: String?,
        payerId: String,
        receiverId: String,
        amount: Double
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {

            postgrest["settlements"]
                .insert(
                    CreateSettlementRequest(
                        sourceType = sourceType,
                        sourceId = sourceId,
                        payerId = payerId,
                        receiverId = receiverId,
                        amount = amount
                    )
                )

            Result.success(Unit)

        } catch (e: Exception) {

            android.util.Log.e(
                "OWEE_SETTLEMENT",
                "Create settlement failed",
                e
            )

            Result.failure(e)
        }
    }

    override suspend fun getSettlements(
        sourceType: String,
        sourceId: String?
    ): List<Settlement> =
        withContext(Dispatchers.IO) {

            try {

                postgrest["settlements"]
                    .select {
                        filter {

                            eq(
                                "source_type",
                                sourceType
                            )

                            if (sourceId != null) {

                                eq(
                                    "source_id",
                                    sourceId
                                )
                            }
                        }

                        order(
                            "created_at",
                            Order.DESCENDING
                        )
                    }
                    .decodeList<Settlement>()

            } catch (e: Exception) {

                android.util.Log.e(
                    "OWEE_SETTLEMENT",
                    "Load settlements failed",
                    e
                )

                emptyList()
            }
        }

    override fun settlementChanges(): Flow<Unit> = flow {
        val channelId = "settlement_changes_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)

        try {
            val postgresFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "settlements"
            }

            client.realtime.connect()
            client.realtime.status.first { it == Realtime.Status.CONNECTED }

            channel.subscribe()
            channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }

            postgresFlow.collect {
                emit(Unit)
            }
        } finally {
            channel.unsubscribe()
            client.realtime.removeChannel(channel)
        }
    }
}