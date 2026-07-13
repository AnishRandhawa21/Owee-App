package com.anish.owee.domain

data class SettlementInstruction(
    val sourceType: String,
    val sourceId: String?,
    val payerId: String,
    val receiverId: String,
    val amount: Double
)
