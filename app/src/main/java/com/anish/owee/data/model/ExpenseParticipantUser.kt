package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseParticipantUser(

    @SerialName("share_amount")
    val shareAmount: Double,

    val user: User? = null
)