package com.anish.owee.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.anish.owee.data.repository.FriendRequestRepository
import com.anish.owee.data.repository.FriendRequestRepositoryImpl
import com.anish.owee.data.repository.SettlementRepository
import com.anish.owee.data.repository.SettlementRepositoryImpl
import com.anish.owee.data.repository.FriendshipRepository
import com.anish.owee.data.repository.FriendshipRepositoryImpl
import com.anish.owee.domain.FriendBalanceCalculator
import com.anish.owee.viewmodel.state.CreateFriendRequestUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.model.OweeNotification
import com.anish.owee.data.repository.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

class CreateFriendRequestViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow(CreateFriendRequestUiState())

    val uiState: StateFlow<CreateFriendRequestUiState> =
        _uiState.asStateFlow()

    fun updateAmount(amount: String) {
        _uiState.value =
            _uiState.value.copy(
                amount = amount
            )
    }

    fun updateNote(note: String) {
        _uiState.value =
            _uiState.value.copy(
                note = note
            )
    }

    private val repository: FriendRequestRepository =
        FriendRequestRepositoryImpl()

    private val settlementRepository: SettlementRepository =
        SettlementRepositoryImpl()

    private val friendshipRepository: FriendshipRepository =
        FriendshipRepositoryImpl()
    
    private val authRepository: AuthRepository = 
        AuthRepositoryImpl()
    
    private val notificationRepository: NotificationRepository = 
        NotificationRepositoryImpl()

    fun createRequest(friendId: String) {

        val amount =
            uiState.value.amount.toDoubleOrNull()

        if (amount == null) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                repository.createRequest(
                    friendId = friendId,
                    amount = amount,
                    note = uiState.value.note.ifBlank { null }
                )

                // SYNC DATABASE STATUSES AFTER CREATING REQUEST
                try {
                    val requests = repository.getRequestsForFriend(friendId)
                    val currentUserId = repository.getCurrentUserId() ?: ""
                    
                    val friendship = friendshipRepository.getAcceptedFriendships().firstOrNull { 
                        it.senderId == friendId || it.receiverId == friendId 
                    }
                    
                    if (friendship != null) {
                        val allocations = settlementRepository.getAllocations("FRIEND", friendship.id)

                        val totalRequestedByMe = requests.filter { it.creatorId == currentUserId }.sumOf { it.amount }
                        val totalRequestedByFriend = requests.filter { it.creatorId != currentUserId }.sumOf { it.amount }
                        
                        val netBalance = FriendBalanceCalculator.calculate(
                            currentUserId = currentUserId,
                            requests = requests,
                            allocations = allocations
                        )
                        
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
                            val rAmount = request.amount
                            
                            val shouldBePaid = if (isOwedToMe) {
                                val covered = kotlin.math.min(rAmount, creditToApplyToMyRequests)
                                creditToApplyToMyRequests -= covered
                                covered >= rAmount - 0.01
                            } else {
                                val covered = kotlin.math.min(rAmount, creditToApplyToFriendRequests)
                                creditToApplyToFriendRequests -= covered
                                covered >= rAmount - 0.01
                            }

                            if (shouldBePaid && request.status == "pending") {
                                repository.markRequestPaid(request.id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OWEE_SYNC", "Request sync failed", e)
                }

                // Send Notification
                viewModelScope.launch {
                    try {
                        val currentUser = authRepository.getCurrentUser()
                        val notification = OweeNotification(
                            senderId = currentUser?.id ?: "",
                            receiverId = friendId,
                            type = "money_request",
                            title = "Settlement Request",
                            body = "${currentUser?.displayName} requested repayment of ₹${String.format(Locale.US, "%.2f", amount)} for \"${uiState.value.note.ifBlank { "Expense" }}\"",
                            data = buildJsonObject {
                                put("type", "money_request")
                                put("payer_name", currentUser?.displayName ?: "Someone")
                                put("amount", amount)
                                put("expense_title", uiState.value.note.ifBlank { "Settlement" })
                                currentUser?.photoUrl?.let { put("sender_photo", it) }
                            }
                        )
                        notificationRepository.sendNotification(notification)
                    } catch (e: Exception) {
                        Log.e("OWEE_NOTIFICATION", "Failed to send money request notification", e)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }

}