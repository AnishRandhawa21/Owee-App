package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSettlementSessionRequest(
    @SerialName("payer_id")
    val payerId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    @SerialName("total_amount")
    val totalAmount: Double,
    val type: String
)
