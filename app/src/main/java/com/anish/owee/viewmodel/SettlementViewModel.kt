package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.AuthRepository
import com.anish.owee.data.repository.AuthRepositoryImpl
import com.anish.owee.data.repository.FriendRequestRepository
import com.anish.owee.data.repository.FriendRequestRepositoryImpl
import com.anish.owee.data.repository.SettlementRepositoryImpl
import com.anish.owee.viewmodel.state.SettlementUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettlementViewModel : ViewModel() {

    private val authRepository: AuthRepository =
        AuthRepositoryImpl()

    private val friendRequestRepository: FriendRequestRepository =
        FriendRequestRepositoryImpl()

    private val _uiState =
        MutableStateFlow(SettlementUiState())


    val uiState: StateFlow<SettlementUiState> =
        _uiState.asStateFlow()

    fun loadSettlementData(
        userId: String,
        amount: Double,
        sourceType: String,
        sourceId: String
    ) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val user =
                    authRepository.getUserById(
                        userId
                    )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        amount = amount,
                        sourceType = sourceType,
                        sourceId = sourceId
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

    fun selectPaymentApp(
        appName: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                selectedApp = appName
            )
    }

    fun setPaymentInProgress(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                isPaymentInProgress = value
            )
    }

    private val settlementRepository =
        SettlementRepositoryImpl()

    fun createSettlement() {

        viewModelScope.launch {

            val currentUser =
                authRepository.getCurrentUser()
                    ?: return@launch

            val targetUser =
                _uiState.value.user
                    ?: return@launch

            val sourceType = _uiState.value.sourceType
            val sourceId = _uiState.value.sourceId

            val result =
                settlementRepository.createSettlement(
                    sourceType = sourceType,
                    sourceId = sourceId,
                    payerId = currentUser.id,
                    receiverId = targetUser.id,
                    amount = _uiState.value.amount
                )

            result.onSuccess {

                android.util.Log.d(
                    "OWEE_SETTLEMENT",
                    "Settlement created"
                )

                // SYNC DATABASE STATUSES (FIFO + Netting logic)
                if (sourceType == "FRIEND") {
                    try {
                        val friendId = targetUser.id
                        val requests = friendRequestRepository.getRequestsForFriend(friendId)
                        val settlements = settlementRepository.getSettlements("FRIEND", sourceId)

                        val currentUserId = currentUser.id
                        
                        val totalRequestedByMe = requests.filter { it.creatorId == currentUserId }.sumOf { it.amount }
                        val totalRequestedByFriend = requests.filter { it.creatorId != currentUserId }.sumOf { it.amount }
                        val totalPaidByMe = settlements.filter { it.payerId == currentUserId }.sumOf { it.amount }
                        val totalReceivedByMe = settlements.filter { it.payerId != currentUserId }.sumOf { it.amount }
                        
                        val netBalance = (totalRequestedByMe - totalRequestedByFriend) + (totalPaidByMe - totalReceivedByMe)
                        
                        var creditToApplyToMyRequests = 0.0
                        var creditToApplyToFriendRequests = 0.0
                        
                        if (netBalance >= -0.01) {
                            creditToApplyToFriendRequests = totalRequestedByFriend
                            creditToApplyToMyRequests = totalRequestedByMe - kotlin.math.max(0.0, netBalance)
                        } else {
                            creditToApplyToMyRequests = totalRequestedByMe
                            creditToApplyToFriendRequests = totalRequestedByFriend - kotlin.math.abs(netBalance)
                        }

                        requests.sortedBy { it.createdAt }.forEach { request ->
                            val isOwedToMe = request.creatorId == currentUserId
                            val amount = request.amount
                            
                            val shouldBePaid = if (isOwedToMe) {
                                val covered = kotlin.math.min(amount, creditToApplyToMyRequests)
                                creditToApplyToMyRequests -= covered
                                covered >= amount - 0.01
                            } else {
                                val covered = kotlin.math.min(amount, creditToApplyToFriendRequests)
                                creditToApplyToFriendRequests -= covered
                                covered >= amount - 0.01
                            }

                            if (shouldBePaid && request.status == "pending") {
                                friendRequestRepository.markRequestPaid(request.id)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OWEE_SYNC", "Database status sync failed", e)
                    }
                }

                _uiState.value =
                    _uiState.value.copy(
                        settlementSuccess = true
                    )
            }

            result.onFailure {

                android.util.Log.e(
                    "OWEE_SETTLEMENT",
                    "Settlement failed",
                    it
                )
            }
        }
    }

    fun showConfirmationDialog() {

        _uiState.value =
            _uiState.value.copy(
                showConfirmationDialog = true
            )
    }

    fun dismissConfirmationDialog() {

        _uiState.value =
            _uiState.value.copy(
                showConfirmationDialog = false,
                isPaymentInProgress = false
            )
    }
}