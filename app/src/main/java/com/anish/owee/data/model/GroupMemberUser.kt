package com.anish.owee.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberUser(
    val user: User? = null
)