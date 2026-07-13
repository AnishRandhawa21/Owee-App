package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettlementAllocation(
    val id: String,
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("source_type")
    val sourceType: String, // 'GROUP' or 'FRIEND'
    @SerialName("source_id")
    val sourceId: String,
    @SerialName("payer_id")
    val payerId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    val amount: Double,
    @SerialName("created_at")
    val createdAt: String
)
