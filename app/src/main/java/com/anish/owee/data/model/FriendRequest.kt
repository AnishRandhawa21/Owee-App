package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(

    @SerialName("id")
    val id: String,

    @SerialName("creator_id")
    val creatorId: String,

    @SerialName("friend_id")
    val friendId: String,

    @SerialName("amount")
    val amount: Double,

    @SerialName("note")
    val note: String? = null,

    @SerialName("status")
    val status: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)