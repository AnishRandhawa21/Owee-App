package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateExpenseParticipantRequest(

    @SerialName("expense_id")
    val expenseId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("share_amount")
    val shareAmount: Double
)