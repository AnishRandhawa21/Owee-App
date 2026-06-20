package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateExpenseRequest(

    @SerialName("group_id")
    val groupId: String,

    @SerialName("payer_id")
    val payerId: String,

    val title: String,

    val amount: Double
)