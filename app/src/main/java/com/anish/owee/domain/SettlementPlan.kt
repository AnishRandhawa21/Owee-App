package com.anish.owee.domain

import com.anish.owee.data.model.CreateSettlementSessionRequest

data class SettlementPlan(
    val session: CreateSettlementSessionRequest,
    val allocations: List<CreateSettlementAllocationPlan>
)

data class CreateSettlementAllocationPlan(
    val sourceType: String,
    val sourceId: String,
    val payerId: String,
    val receiverId: String,
    val amount: Double
)
