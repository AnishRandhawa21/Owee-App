package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.repository.*
import com.anish.owee.domain.FriendBalanceCalculator
import com.anish.owee.viewmodel.state.FriendshipUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FriendshipViewModel(application: Application) : AndroidViewModel(application) {

    private val friendshipRepository: FriendshipRepository =
        FriendshipRepositoryImpl()

    private val userSearchRepository: UserSearchRepository =
        UserSearchRepositoryImpl()

    private val friendRequestRepository: FriendRequestRepository =
        FriendRequestRepositoryImpl()

    private val settlementRepository: SettlementRepository =
        SettlementRepositoryImpl()
    
    private val authRepository: AuthRepository = 
        AuthRepositoryImpl()
    
    private val notificationRepository: NotificationRepository = 
        NotificationRepositoryImpl()

    private val preferenceManager = PreferenceManager(application)

    private val _uiState =
        MutableStateFlow(FriendshipUiState())

    val uiState: StateFlow<FriendshipUiState> =
        _uiState.asStateFlow()

    init {
        loadCachedFriends()
        loadData()
        observeFriendshipChanges()
    }

    private fun loadCachedFriends() {
        val currentUserId = friendshipRepository.getCurrentUserId()
        val cached = preferenceManager.getFriends()
        val cachedBalances = preferenceManager.getFriendBalances()
        if (cached.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                friends = cached,
                friendBalances = cachedBalances,
                currentUserId = currentUserId,
                isLoading = false
            )
        }
    }

    private fun observeFriendshipChanges() {
        viewModelScope.launch {
            com.anish.owee.data.remote.SupabaseProvider.ensureRealtimeConnected()
            merge(
                friendshipRepository.friendshipChanges(),
                friendRequestRepository.requestChanges(),
                settlementRepository.settlementChanges()
            ).collectLatest {
                android.util.Log.d(
                    "OWEE_REALTIME",
                    "Friendship screen data change detected"
                )
                loadData(isSilent = true)
            }
        }
    }

    fun loadData(isSilent: Boolean = false) {
        viewModelScope.launch {

            if (_uiState.value.friends.isEmpty() && !isSilent) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
            }

            try {

                val incoming =
                    friendshipRepository.getIncomingRequests()

                val outgoing =
                    friendshipRepository.getOutgoingRequests()

                val friends =
                    friendshipRepository.getAcceptedFriendships()

                val currentUserId = friendshipRepository.getCurrentUserId() ?: ""
                val balances = mutableMapOf<String, Double>()
                
                friends.forEach { friendship ->
                    val friendId = if (friendship.senderId == currentUserId) friendship.receiverId else friendship.senderId
                    val requests = friendRequestRepository.getRequestsForFriend(friendId)
                    val settlements = settlementRepository.getSettlements("FRIEND", friendship.id)
                    
                    balances[friendship.id] = FriendBalanceCalculator.calculate(
                        currentUserId = currentUserId,
                        requests = requests,
                        settlements = settlements
                    )
                }

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        incomingRequests = incoming,
                        outgoingRequests = outgoing,
                        friends = friends,
                        friendBalances = balances,
                        currentUserId = currentUserId
                    )

                // Save to cache
                preferenceManager.saveFriends(friends, balances)

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
                searchQuery = query,
                searchResults = emptyList(),
                hasSearched = false
            )
    }

    fun searchUsers() {
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isSearching = true, hasSearched = false)

            try {
                val currentUserId = _uiState.value.currentUserId
                val results =
                    userSearchRepository.searchUsers(
                        _uiState.value.searchQuery
                    ).filter { it.id != currentUserId }

                _uiState.value =
                    _uiState.value.copy(
                        searchResults = results,
                        isSearching = false,
                        hasSearched = true
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error = e.message,
                        isSearching = false,
                        hasSearched = true
                    )
            }
        }
    }

    fun sendFriendRequest(receiverId: String) {
        viewModelScope.launch {

            val result = friendshipRepository
                .sendFriendRequest(receiverId)

            result.onSuccess {
                viewModelScope.launch {
                    try {
                        val currentUser = authRepository.getCurrentUser()
                        val notification = com.anish.owee.data.model.OweeNotification(
                            senderId = currentUser?.id ?: "",
                            receiverId = receiverId,
                            type = "friend_request",
                            title = "Friend Request",
                            body = "${currentUser?.displayName} sent you a friend request",
                            data = buildJsonObject {
                                put("type", "friend_request")
                                put("payer_name", currentUser?.displayName ?: "Someone")
                                currentUser?.photoUrl?.let { put("sender_photo", it) }
                            }
                        )
                        notificationRepository.sendNotification(notification)
                    } catch (e: Exception) {
                        android.util.Log.e("OWEE_NOTIFICATION", "Failed to send friend request notification", e)
                    }
                }
            }

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

    fun validateFriendRemoval(friendshipId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, validationBalance = null)

            try {
                val currentUserId = friendshipRepository.getCurrentUserId() ?: return@launch
                val friendship = _uiState.value.friends.firstOrNull { it.id == friendshipId } ?: return@launch
                val friendId = if (friendship.senderId == currentUserId) friendship.receiverId else friendship.senderId

                val requests = friendRequestRepository.getRequestsForFriend(friendId)
                val settlements = settlementRepository.getSettlements("FRIEND", friendshipId)

                val netBalance = FriendBalanceCalculator.calculate(
                    currentUserId = currentUserId,
                    requests = requests,
                    settlements = settlements
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationBalance = netBalance
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = friendshipRepository.removeFriendship(friendshipId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    friends = _uiState.value.friends.filter { it.id != friendshipId },
                    validationBalance = null
                )
            }

            result.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not remove friend. Please try again."
                )
            }
        }
    }

    fun resetValidation() {
        _uiState.value = _uiState.value.copy(validationBalance = null)
    }
}
