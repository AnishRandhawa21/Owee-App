package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.FriendRequest
import com.anish.owee.data.model.Settlement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FriendActivity(
    val id: String,
    val title: String,
    val note: String?,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val type: String, // "request" or "settlement"
    val creatorId: String // Add this for deletion ownership check
)

@Serializable
data class FriendRequestUiState(

    @Transient
    val isLoading: Boolean = false,

    val requests: List<FriendRequest> = emptyList(),

    val settlements: List<Settlement> = emptyList(),

    val activities: List<FriendActivity> = emptyList(),

    val balance: Double = 0.0,

    val requestedByMe: Double = 0.0,

    val requestedByFriend: Double = 0.0,

    @Transient
    val error: String? = null
)