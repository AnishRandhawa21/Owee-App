package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.Friendship

data class CreateGroupUiState(
    val groupName: String = "",
    val friends: List<Friendship> = emptyList(),
    val selectedFriendIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)