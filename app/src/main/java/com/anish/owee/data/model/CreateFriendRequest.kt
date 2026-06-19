package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateFriendRequest(

    @SerialName("creator_id")
    val creatorId: String,

    @SerialName("friend_id")
    val friendId: String,

    @SerialName("amount")
    val amount: Double,

    @SerialName("note")
    val note: String?,

    @SerialName("status")
    val status: String = "pending"
)