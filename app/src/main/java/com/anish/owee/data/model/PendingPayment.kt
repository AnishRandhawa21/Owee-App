package com.anish.owee.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PendingPayment(
    val amount: Double,
    val recipientId: String,
    val recipientName: String,
    val sourceType: String, // "GROUP" or "FRIEND"
    val sourceId: String,
    val timestamp: Long
)
