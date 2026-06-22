package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.Friendship
import com.anish.owee.data.model.SearchUser

data class FriendshipUiState(
    val isLoading: Boolean = false,

    val incomingRequests: List<Friendship> = emptyList(),

    val outgoingRequests: List<Friendship> = emptyList(),

    val friends: List<Friendship> = emptyList(),

    val searchQuery: String = "",

    val searchResults: List<SearchUser> = emptyList(),

    val isSearching: Boolean = false,

    val hasSearched: Boolean = false,

    val currentUserId: String? = null,

    val validationBalance: Double? = null,

    val friendBalances: Map<String, Double> = emptyMap(),

    val error: String? = null
)