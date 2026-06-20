package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.FriendshipRepository
import com.anish.owee.data.repository.FriendshipRepositoryImpl
import com.anish.owee.data.repository.GroupRepository
import com.anish.owee.data.repository.GroupRepositoryImpl
import com.anish.owee.viewmodel.state.CreateGroupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateGroupViewModel : ViewModel() {

    private val friendshipRepository: FriendshipRepository =
        FriendshipRepositoryImpl()

    private val groupRepository: GroupRepository =
        GroupRepositoryImpl()

    private val _uiState =
        MutableStateFlow(CreateGroupUiState())

    private val currentUserId =
        friendshipRepository.getCurrentUserId()


    val uiState: StateFlow<CreateGroupUiState> =
        _uiState.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(isLoading = true)

            try {

                val friendships =
                    friendshipRepository.getAcceptedFriendships()

                val currentUserId =
                    friendshipRepository.getCurrentUserId()

                val friends =
                    friendships.filter {
                        currentUserId != null
                    }

                _uiState.value =
                    _uiState.value.copy(
                        friends = friends,
                        isLoading = false
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

    fun updateGroupName(name: String) {
        _uiState.value =
            _uiState.value.copy(
                groupName = name
            )
    }

    fun toggleFriend(friendId: String) {

        val selected =
            _uiState.value.selectedFriendIds.toMutableSet()

        if (selected.contains(friendId)) {
            selected.remove(friendId)
        } else {
            selected.add(friendId)
        }

        _uiState.value =
            _uiState.value.copy(
                selectedFriendIds = selected
            )
    }

    fun createGroup() {

        if (_uiState.value.groupName.isBlank()) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true
                )

            val result =
                groupRepository.createGroup(
                    name = _uiState.value.groupName,
                    memberIds = _uiState.value.selectedFriendIds.toList()
                )

            result.onSuccess {

                android.util.Log.d(
                    "OWEE_GROUP",
                    "Group created successfully"
                )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
            }

            result.onFailure {

                android.util.Log.e(
                    "OWEE_GROUP",
                    "Create group failed",
                    it
                )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = it.message
                    )
            }
        }
    }

    fun getFriendUser(friendship: com.anish.owee.data.model.Friendship)
            : com.anish.owee.data.model.User? {

        val currentUserId =
            friendshipRepository.getCurrentUserId()

        return if (friendship.senderId == currentUserId) {
            friendship.receiver
        } else {
            friendship.sender
        }
    }
}