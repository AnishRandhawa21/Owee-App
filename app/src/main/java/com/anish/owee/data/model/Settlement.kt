package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Settlement(

    val id: String,

    @SerialName("source_type")
    val sourceType: String,

    @SerialName("source_id")
    val sourceId: String?,

    @SerialName("payer_id")
    val payerId: String,

    @SerialName("receiver_id")
    val receiverId: String,

    val amount: Double,

    val status: String,

    @SerialName("created_at")
    val createdAt: String
)