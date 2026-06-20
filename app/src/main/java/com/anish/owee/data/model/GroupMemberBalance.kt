package com.anish.owee.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberBalance(

    val userId: String,

    val amount: Double
)