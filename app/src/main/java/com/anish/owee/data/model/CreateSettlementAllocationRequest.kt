package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSettlementAllocationRequest(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("source_type")
    val sourceType: String,
    @SerialName("source_id")
    val sourceId: String,
    @SerialName("payer_id")
    val payerId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    val amount: Double
)
