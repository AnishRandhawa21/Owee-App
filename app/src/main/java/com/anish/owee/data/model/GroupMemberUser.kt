package com.anish.owee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberUser(
    @SerialName("group_id")
    val groupId: String = "",
    val user: User? = null
)