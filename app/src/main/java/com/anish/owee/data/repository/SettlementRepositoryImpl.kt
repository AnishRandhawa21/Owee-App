package com.anish.owee.data.repository

import com.anish.owee.data.model.*
import com.anish.owee.data.remote.SupabaseProvider
import com.anish.owee.domain.SettlementPlan
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

    override suspend fun createSettlementSession(
        plan: SettlementPlan
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Insert Session
            val session = postgrest["settlement_sessions"]
                .insert(plan.session) {
                    select()
                }
                .decodeSingle<SettlementSession>()

            // 2. Map Allocations to Session ID and use explicit directional IDs from plan
            val allocations = plan.allocations.map {
                CreateSettlementAllocationRequest(
                    sessionId = session.id,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    payerId = it.payerId,
                    receiverId = it.receiverId,
                    amount = it.amount
                )
            }

            // 3. Insert Allocations
            if (allocations.isNotEmpty()) {
                postgrest["settlement_allocations"].insert(allocations)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("OWEE_SETTLEMENT", "Create session failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getAllocations(
        sourceType: String,
        sourceId: String
    ): List<SettlementAllocation> = withContext(Dispatchers.IO) {
        try {
            postgrest["settlement_allocations"]
                .select {
                    filter {
                        eq("source_type", sourceType)
                        eq("source_id", sourceId)
                    }
                }
                .decodeList<SettlementAllocation>()
        } catch (e: Exception) {
            android.util.Log.e("OWEE_SETTLEMENT", "Load allocations failed", e)
            emptyList()
        }
    }

    override suspend fun getSessions(
        userId: String
    ): List<SettlementSession> = withContext(Dispatchers.IO) {
        try {
            postgrest["settlement_sessions"]
                .select {
                    filter {
                        or {
                            eq("payer_id", userId)
                            eq("receiver_id", userId)
                        }
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<SettlementSession>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteSettlement(settlementId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Note: We are deleting from legacy settlements table for compatibility
            postgrest["settlements"]
                .delete {
                    filter {
                        eq("id", settlementId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("OWEE_SETTLEMENT", "Delete settlement failed", e)
            Result.failure(e)
        }
    }

    override fun settlementChanges(): Flow<Unit> = flow {
        val channelId = "settlement_changes_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)

        try {
            android.util.Log.d("OWEE_REALTIME", "Attempting to subscribe to settlement tables")

            val sessionsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "settlement_sessions"
            }
            
            val allocationsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "settlement_allocations"
            }

            channel.subscribe()
            channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }
            
            android.util.Log.d("OWEE_REALTIME", "Subscribed successfully to settlement tables")

            kotlinx.coroutines.flow.merge(sessionsFlow, allocationsFlow).collect {
                android.util.Log.d("OWEE_REALTIME", "Change detected in settlements")
                emit(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("OWEE_REALTIME", "Error in settlementChanges flow", e)
        } finally {
            channel.unsubscribe()
            client.realtime.removeChannel(channel)
        }
    }
}
