package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import com.anish.owee.data.repository.FriendRequestRepository
import com.anish.owee.data.repository.FriendRequestRepositoryImpl
import com.anish.owee.viewmodel.state.FriendRequestUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class FriendRequestViewModel : ViewModel() {

    private val repository: FriendRequestRepository =
        FriendRequestRepositoryImpl()

    private val _uiState =
        MutableStateFlow(FriendRequestUiState())

    val uiState: StateFlow<FriendRequestUiState> =
        _uiState.asStateFlow()

    fun loadRequests(
        friendId: String
    ) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val requests =
                    repository.getRequestsForFriend(friendId)


                val currentUserId =
                    repository.getCurrentUserId()

                val requestedByMe =
                    requests
                        .filter { it.creatorId == currentUserId }
                        .sumOf { it.amount }

                val requestedByFriend =
                    requests
                        .filter { it.creatorId != currentUserId }
                        .sumOf { it.amount }

                val balance = requests
                    .filter { it.status == "pending" }
                    .sumOf { request ->

                        if (request.creatorId == currentUserId) {
                            request.amount
                        } else {
                            -request.amount
                        }
                    }

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        requests = requests,
                        balance = balance,
                        requestedByMe = requestedByMe,
                        requestedByFriend = requestedByFriend
                    )
                android.util.Log.d(
                    "OWEE_REQUESTS",
                    "Loaded ${requests.size} requests"
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
    fun markRequestPaid(
        requestId: String,
        friendId: String
    ) {
        viewModelScope.launch {

            repository.markRequestPaid(requestId)

            loadRequests(friendId)
        }
    }
}