package com.anish.owee.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.AuthRepository
import com.anish.owee.data.repository.AuthRepositoryImpl
import com.anish.owee.data.repository.FriendRequestRepository
import com.anish.owee.data.repository.FriendRequestRepositoryImpl
import com.anish.owee.data.repository.NotificationRepository
import com.anish.owee.data.repository.NotificationRepositoryImpl
import com.anish.owee.data.repository.SettlementRepositoryImpl
import com.anish.owee.domain.FriendBalanceCalculator
import com.anish.owee.data.model.OweeNotification
import com.anish.owee.utils.UpiPaymentManager
import com.anish.owee.viewmodel.state.SettlementUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SettlementViewModel : ViewModel() {

    private val authRepository: AuthRepository = AuthRepositoryImpl()

    private val friendRequestRepository: FriendRequestRepository = FriendRequestRepositoryImpl()

    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl()

    private val _uiState = MutableStateFlow(SettlementUiState())

    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    fun loadSettlementData(
        userId: String,
        amount: Double,
        sourceType: String,
        sourceId: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val user = authRepository.getUserById(userId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    amount = amount,
                    sourceType = sourceType,
                    sourceId = sourceId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectPaymentApp(appName: String) {
        _uiState.value = _uiState.value.copy(selectedApp = appName)
    }

    fun loadInstalledUpiApps(context: Context) {
        val apps = UpiPaymentManager.getInstalledUpiApps(context)
        _uiState.value = _uiState.value.copy(
            installedUpiApps = apps,
            selectedApp = apps.firstOrNull()?.packageName
        )
    }

    fun setPaymentInProgress(value: Boolean) {
        _uiState.value = _uiState.value.copy(isPaymentInProgress = value)
    }

    private val settlementRepository = SettlementRepositoryImpl()

    fun createSettlement() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch
            val targetUser = _uiState.value.user ?: return@launch
            val sourceType = _uiState.value.sourceType
            val sourceId = _uiState.value.sourceId

            val result = settlementRepository.createSettlement(
                sourceType = sourceType,
                sourceId = sourceId,
                payerId = currentUser.id,
                receiverId = targetUser.id,
                amount = _uiState.value.amount
            )

            result.onSuccess {
                android.util.Log.d("OWEE_SETTLEMENT", "Settlement created")
                
                // Send Notification
                viewModelScope.launch {
                    try {
                        val notification = OweeNotification(
                            senderId = currentUser.id,
                            receiverId = targetUser.id,
                            type = "settlement",
                            title = "Payment Received",
                            body = "${currentUser.displayName} settled ₹${String.format(java.util.Locale.US, "%.2f", _uiState.value.amount)}",
                            data = buildJsonObject {
                                put("type", "settlement")
                                put("payer_name", currentUser.displayName)
                                put("amount", _uiState.value.amount)
                                currentUser.photoUrl?.let { put("sender_photo", it) }
                            }
                        )
                        notificationRepository.sendNotification(notification)
                    } catch (e: Exception) {
                        android.util.Log.e("OWEE_NOTIFICATION", "Failed to send settlement notification", e)
                    }
                }

                if (sourceType == "FRIEND") {
                    try {
                        val friendId = targetUser.id
                        val requests = friendRequestRepository.getRequestsForFriend(friendId)
                        val settlements = settlementRepository.getSettlements("FRIEND", sourceId)
                        val currentUserId = currentUser.id
                        
                        val totalRequestedByMe = requests.filter { it.creatorId == currentUserId }.sumOf { it.amount }
                        val totalRequestedByFriend = requests.filter { it.creatorId != currentUserId }.sumOf { it.amount }
                        
                        val netBalance = FriendBalanceCalculator.calculate(
                            currentUserId = currentUserId,
                            requests = requests,
                            settlements = settlements
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
                _uiState.value = _uiState.value.copy(settlementSuccess = true)
            }

            result.onFailure {
                android.util.Log.e("OWEE_SETTLEMENT", "Settlement failed", it)
            }
        }
    }

    fun showConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showConfirmationDialog = true)
    }

    fun dismissConfirmationDialog() {
        _uiState.value = _uiState.value.copy(
            showConfirmationDialog = false,
            isPaymentInProgress = false
        )
    }

    fun dismissTargetUpiDialog() {
        _uiState.value = _uiState.value.copy(showTargetUpiMissingDialog = false)
    }

    fun handlePayClick(context: Context) {
        val user = _uiState.value.user
        val selectedPackage = _uiState.value.selectedApp
        
        if (user?.upiId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(showTargetUpiMissingDialog = true)
            
            // Send a notification to the person who is missing their UPI ID
            viewModelScope.launch {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null && user != null) {
                    val notification = OweeNotification(
                        senderId = currentUser.id,
                        receiverId = user.id,
                        type = "upi_alert",
                        title = "UPI ID Missing",
                        body = "${currentUser.displayName} tried to pay you. Add your UPI ID to receive it."
                    )
                    notificationRepository.sendNotification(notification)
                }
            }
        } else if (selectedPackage != null) {
            UpiPaymentManager.copyUpiId(context, user.upiId!!)
            UpiPaymentManager.launchUpiApp(context, selectedPackage)
            setPaymentInProgress(true)
        }
    }
}
