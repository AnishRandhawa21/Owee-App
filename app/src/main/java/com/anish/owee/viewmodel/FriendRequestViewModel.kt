package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.repository.FriendRequestRepository
import com.anish.owee.data.repository.FriendRequestRepositoryImpl
import com.anish.owee.data.repository.FriendshipRepository
import com.anish.owee.data.repository.FriendshipRepositoryImpl
import com.anish.owee.data.repository.SettlementRepository
import com.anish.owee.data.repository.SettlementRepositoryImpl
import com.anish.owee.viewmodel.state.FriendActivity
import com.anish.owee.viewmodel.state.FriendRequestUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class FriendRequestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FriendRequestRepository =
        FriendRequestRepositoryImpl()

    private val friendshipRepository: FriendshipRepository =
        FriendshipRepositoryImpl()

    private val settlementRepository: SettlementRepository =
        SettlementRepositoryImpl()

    private val preferenceManager = PreferenceManager(application)

    private val _uiState =
        MutableStateFlow(FriendRequestUiState())

    val uiState: StateFlow<FriendRequestUiState> =
        _uiState.asStateFlow()

    private var currentFriendId: String? = null
    private var observationJob: kotlinx.coroutines.Job? = null

    fun loadCachedFriendData(friendId: String) {
        if (currentFriendId == friendId) return
        currentFriendId = friendId
        
        val cached = preferenceManager.getFriendDetail(friendId)
        if (cached != null) {
            _uiState.value = cached.copy(isLoading = false)
        }
        observeChanges(friendId)
    }

    private fun observeChanges(friendId: String) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            com.anish.owee.data.remote.SupabaseProvider.ensureRealtimeConnected()
            merge(
                repository.requestChanges(),
                settlementRepository.settlementChanges(),
                friendshipRepository.friendshipChanges()
            ).collectLatest {
                loadRequests(friendId)
            }
        }
    }

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
                val currentUserId =
                    repository.getCurrentUserId()
                        ?: return@launch

                val requests =
                    repository.getRequestsForFriend(friendId)

                // Get the friendshipId to fetch shared settlements
                val friendship = friendshipRepository.getAcceptedFriendships().firstOrNull { 
                    it.senderId == friendId || it.receiverId == friendId 
                }
                
                val settlements = if (friendship != null) {
                    settlementRepository.getSettlements(
                        sourceType = "FRIEND",
                        sourceId = friendship.id
                    )
                } else {
                    emptyList()
                }

                // 1. Unified Activity List with Mutual Netting logic
                val sortedRequests = requests.sortedBy { it.createdAt }
                
                // Final Ledger Calculation
                val totalRequestedByMe = requests.filter { it.creatorId == currentUserId }.sumOf { it.amount }
                val totalRequestedByFriend = requests.filter { it.creatorId != currentUserId }.sumOf { it.amount }
                val totalPaidByMe = settlements.filter { it.payerId == currentUserId }.sumOf { it.amount }
                val totalReceivedByMe = settlements.filter { it.payerId != currentUserId }.sumOf { it.amount }
                
                val netBalance = (totalRequestedByMe - totalRequestedByFriend) + (totalPaidByMe - totalReceivedByMe)
                
                // Determine how much of the debt/credit has been "Cleared"
                var creditToApplyToMyRequests = 0.0
                var creditToApplyToFriendRequests = 0.0
                
                if (netBalance >= -0.01) {
                    // Friend owes me or we are settled. 
                    // This means ALL of the friend's requests to me are effectively "PAID" (netted out).
                    creditToApplyToFriendRequests = totalRequestedByFriend
                    // My requests to the friend are paid except for the remaining positive balance.
                    creditToApplyToMyRequests = totalRequestedByMe - kotlin.math.max(0.0, netBalance)
                } else {
                    // I owe the friend.
                    // This means ALL my requests to the friend are "PAID" (netted out).
                    creditToApplyToMyRequests = totalRequestedByMe
                    // Friend's requests to me are paid except for the remaining negative balance.
                    creditToApplyToFriendRequests = totalRequestedByFriend - kotlin.math.abs(netBalance)
                }

                val activities = mutableListOf<FriendActivity>()
                
                sortedRequests.forEach { request ->
                    val isOwedToMe = request.creatorId == currentUserId
                    
                    val displayStatus = if (isOwedToMe) {
                        val amountCovered = kotlin.math.min(request.amount, creditToApplyToMyRequests)
                        creditToApplyToMyRequests -= amountCovered
                        if (amountCovered >= request.amount - 0.01) "paid" else request.status
                    } else {
                        val amountCovered = kotlin.math.min(request.amount, creditToApplyToFriendRequests)
                        creditToApplyToFriendRequests -= amountCovered
                        if (amountCovered >= request.amount - 0.01) "paid" else request.status
                    }

                    activities.add(
                        FriendActivity(
                            id = request.id,
                            title = if (isOwedToMe) "You requested" else "Requested from you",
                            note = request.note,
                            amount = request.amount,
                            status = displayStatus,
                            createdAt = request.createdAt,
                            type = "request",
                            creatorId = request.creatorId
                        )
                    )
                }
                
                settlements.forEach { settlement ->
                    activities.add(
                        FriendActivity(
                            id = settlement.id,
                            title = if (settlement.payerId == currentUserId) "You paid" else "You received",
                            note = "Settlement via UPI",
                            amount = settlement.amount,
                            status = "paid",
                            createdAt = settlement.createdAt,
                            type = "settlement",
                            creatorId = settlement.payerId
                        )
                    )
                }

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        requests = requests.sortedByDescending { it.createdAt },
                        settlements = settlements.sortedByDescending { it.createdAt },
                        activities = activities.sortedByDescending { it.createdAt },
                        balance = netBalance,
                        requestedByMe = totalRequestedByMe,
                        requestedByFriend = totalRequestedByFriend
                    )

                // Save to cache
                preferenceManager.saveFriendDetail(friendId, _uiState.value)

                android.util.Log.d(
                    "OWEE_FRIEND_BALANCE",
                    "FriendId=$friendId, Balance=$netBalance"
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

    fun getCurrentUserId(): String? = repository.getCurrentUserId()

    fun deleteActivity(
        activity: FriendActivity,
        friendId: String
    ) {
        viewModelScope.launch {
            val currentUserId = repository.getCurrentUserId()
            if (activity.creatorId != currentUserId) {
                _uiState.value = _uiState.value.copy(error = "You can only delete your own entries")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = if (activity.type == "request") {
                repository.deleteRequest(activity.id)
            } else {
                settlementRepository.deleteSettlement(activity.id)
            }

            result.onSuccess {
                loadRequests(friendId)
            }
            result.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }
}
