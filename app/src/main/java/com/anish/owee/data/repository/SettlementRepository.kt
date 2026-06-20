package com.anish.owee.data.repository

import com.anish.owee.data.model.Settlement

interface SettlementRepository {

    suspend fun createSettlement(
        sourceType: String,
        sourceId: String?,
        payerId: String,
        receiverId: String,
        amount: Double
    ): Result<Unit>

    suspend fun getSettlements(
        sourceType: String,
        sourceId: String?
    ): List<Settlement>
}