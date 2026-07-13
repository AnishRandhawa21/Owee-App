package com.anish.owee.data.repository

import com.anish.owee.data.model.Settlement
import com.anish.owee.data.model.SettlementAllocation
import com.anish.owee.data.model.SettlementSession
import com.anish.owee.domain.SettlementPlan

interface SettlementRepository {

    suspend fun createSettlementSession(
        plan: SettlementPlan
    ): Result<Unit>

    suspend fun getAllocations(
        sourceType: String,
        sourceId: String
    ): List<SettlementAllocation>

    suspend fun getSessions(
        userId: String
    ): List<SettlementSession>

    fun settlementChanges(): kotlinx.coroutines.flow.Flow<Unit>

    suspend fun deleteSettlement(settlementId: String): Result<Unit>
}
