package com.anish.owee.data.repository

import com.anish.owee.data.model.CreateSettlementRequest
import com.anish.owee.data.model.Settlement
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
}