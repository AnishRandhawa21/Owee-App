package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.FriendRequest

data class FriendRequestUiState(

    val isLoading: Boolean = false,

    val requests: List<FriendRequest> = emptyList(),

    val balance: Double = 0.0,

    val requestedByMe: Double = 0.0,

    val requestedByFriend: Double = 0.0,

    val error: String? = null
)