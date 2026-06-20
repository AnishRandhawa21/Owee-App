package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(

    val id: String,

    @SerialName("group_id")
    val groupId: String,

    @SerialName("payer_id")
    val payerId: String,

    val title: String,

    val amount: Double,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)