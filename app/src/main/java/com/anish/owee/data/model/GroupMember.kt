package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
    val id: String,

    @SerialName("group_id")
    val groupId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("joined_at")
    val joinedAt: String
)