package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.FriendshipRepository
import com.anish.owee.data.repository.FriendshipRepositoryImpl
import com.anish.owee.data.repository.UserSearchRepository
import com.anish.owee.data.repository.UserSearchRepositoryImpl
import com.anish.owee.viewmodel.state.FriendshipUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FriendshipViewModel : ViewModel() {

    private val friendshipRepository: FriendshipRepository =
        FriendshipRepositoryImpl()

    private val userSearchRepository: UserSearchRepository =
        UserSearchRepositoryImpl()

    private val _uiState =
        MutableStateFlow(FriendshipUiState())

    val uiState: StateFlow<FriendshipUiState> =
        _uiState.asStateFlow()

    init {
        loadData()
        observeFriendshipChanges()
    }

    private fun observeFriendshipChanges() {
        viewModelScope.launch {
            friendshipRepository.friendshipChanges()
                .collectLatest {

                    android.util.Log.d(
                        "OWEE_REALTIME",
                        "Friendship change received"
                    )

                    loadData()
                }
        }
    }

    fun loadData() {
        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val incoming =
                    friendshipRepository.getIncomingRequests()

                val outgoing =
                    friendshipRepository.getOutgoingRequests()

                val friends =
                    friendshipRepository.getAcceptedFriendships()

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        incomingRequests = incoming,
                        outgoingRequests = outgoing,
                        friends = friends,
                        currentUserId = friendshipRepository.getCurrentUserId()
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value =
            _uiState.value.copy(
                searchQuery = query
            )
    }

    fun searchUsers() {
        viewModelScope.launch {

            try {

                val results =
                    userSearchRepository.searchUsers(
                        _uiState.value.searchQuery
                    )

                _uiState.value =
                    _uiState.value.copy(
                        searchResults = results
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error = e.message
                    )
            }
        }
    }

    fun sendFriendRequest(receiverId: String) {
        viewModelScope.launch {

            friendshipRepository
                .sendFriendRequest(receiverId)

            loadData()
        }
    }

    fun acceptFriendRequest(friendshipId: String) {
        viewModelScope.launch {

            friendshipRepository
                .acceptFriendRequest(friendshipId)

            loadData()
        }
    }

    fun rejectFriendRequest(friendshipId: String) {
        viewModelScope.launch {

            friendshipRepository
                .rejectFriendRequest(friendshipId)

            loadData()
        }
    }
}