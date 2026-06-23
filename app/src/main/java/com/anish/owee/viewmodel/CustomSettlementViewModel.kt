package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.model.PendingPayment
import com.anish.owee.data.model.User
import com.anish.owee.data.repository.*
import com.anish.owee.domain.FriendBalanceCalculator
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.utils.PaymentReminderManager
import com.anish.owee.utils.UpiApp
import com.anish.owee.utils.UpiPaymentManager
import com.anish.owee.viewmodel.state.DebtSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

data class CustomSettlementUiState(
    val isLoading: Boolean = false,
    val targetUser: User? = null,
    val totalDebt: Double = 0.0,
    val amount: String = "",
    val sources: List<DebtSource> = emptyList(),
    val installedUpiApps: List<UpiApp> = emptyList(),
    val selectedApp: String? = null,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isPaymentInProgress: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val showUpiMissingDialog: Boolean = false,
    val currentUser: User? = null
)

class CustomSettlementViewModel(application: Application) : AndroidViewModel(application) {

    private val groupRepository: GroupRepository = GroupRepositoryImpl()
    private val expenseRepository: ExpenseRepository = ExpenseRepositoryImpl()
    private val settlementRepository: SettlementRepository = SettlementRepositoryImpl()
    private val friendshipRepository: FriendshipRepository = FriendshipRepositoryImpl()
    private val friendRequestRepository: FriendRequestRepository = FriendRequestRepositoryImpl()
    private val authRepository: AuthRepository = AuthRepositoryImpl()
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl()

    private val _uiState = MutableStateFlow(CustomSettlementUiState())
    val uiState: StateFlow<CustomSettlementUiState> = _uiState.asStateFlow()

    init {
        loadInstalledUpiApps()
    }

    private fun loadInstalledUpiApps() {
        val apps = UpiPaymentManager.getInstalledUpiApps(getApplication())
        _uiState.value = _uiState.value.copy(
            installedUpiApps = apps,
            selectedApp = apps.firstOrNull()?.packageName
        )
    }

    fun selectPaymentApp(packageName: String) {
        _uiState.value = _uiState.value.copy(selectedApp = packageName)
    }

    fun loadUserDebts(targetUserId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                coroutineScope {
                    val currentUserId = groupRepository.getCurrentUserId() ?: return@coroutineScope
                    val userDeferred = async { authRepository.getUserById(targetUserId) }
                    val currentUserDeferred = async { authRepository.getCurrentUser() }
                    val groupsDeferred = async { groupRepository.getGroupsWithMetadata() }
                    
                    val groups = groupsDeferred.await()
                    val user = userDeferred.await()
                    val currentUser = currentUserDeferred.await()

                    val groupSourcesDeferred = groups.map { groupMetadata ->
                        async {
                            val groupId = groupMetadata.group.id
                            val expenses = expenseRepository.getGroupExpenses(groupId)
                            val settlements = settlementRepository.getSettlements("GROUP", groupId)
                            val allParticipants = expenseRepository.getGroupExpenseParticipants(groupId)
                            val participantsByExpense = allParticipants.groupBy { it.expenseId }

                            val groupBalances = GroupBalanceCalculator.calculateBalances(
                                currentUserId = currentUserId,
                                expenses = expenses,
                                participantsByExpense = participantsByExpense,
                                settlements = settlements
                            )

                            groupBalances.find { it.userId == targetUserId }?.let { gb ->
                                if (kotlin.math.abs(gb.amount) > 0.01) {
                                    DebtSource(
                                        sourceType = "GROUP",
                                        sourceId = groupId,
                                        amount = gb.amount,
                                        createdAt = groupMetadata.group.createdAt
                                    )
                                } else null
                            }
                        }
                    }

                    val friendSourceDeferred = async {
                        val friendship = friendshipRepository.getFriendships().firstOrNull {
                            it.senderId == targetUserId || it.receiverId == targetUserId
                        }

                        if (friendship != null) {
                            val requests = friendRequestRepository.getRequestsForFriend(targetUserId)
                            val settlements = settlementRepository.getSettlements("FRIEND", friendship.id)

                            val friendNet = FriendBalanceCalculator.calculate(
                                currentUserId = currentUserId,
                                requests = requests,
                                settlements = settlements
                            )

                            if (kotlin.math.abs(friendNet) > 0.01) {
                                DebtSource(
                                    sourceType = "FRIEND",
                                    sourceId = friendship.id,
                                    amount = friendNet,
                                    createdAt = friendship.createdAt
                                )
                            } else null
                        } else null
                    }

                    val sources = mutableListOf<DebtSource>()
                    sources.addAll(groupSourcesDeferred.awaitAll().filterNotNull())
                    friendSourceDeferred.await()?.let { sources.add(it) }

                    val sortedSources = sources.sortedBy { it.createdAt } // FIFO (Oldest first)
                    val total = sortedSources.sumOf { it.amount }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        targetUser = user,
                        currentUser = currentUser,
                        totalDebt = total,
                        amount = if (total < -0.01) String.format(Locale.US, "%.2f", kotlin.math.abs(total)) else "",
                        sources = sortedSources
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun setAmountRatio(ratio: Double) {
        val totalToPay = kotlin.math.abs(_uiState.value.totalDebt)
        val calculated = totalToPay * ratio
        val rounded = String.format(Locale.US, "%.2f", calculated)
        _uiState.value = _uiState.value.copy(amount = rounded)
    }

    fun setPaymentInProgress(value: Boolean) {
        _uiState.value = _uiState.value.copy(isPaymentInProgress = value)
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

    fun dismissUpiDialog() {
        _uiState.value = _uiState.value.copy(showUpiMissingDialog = false)
    }

    fun handlePaymentClick(context: android.content.Context) {
        val targetUser = _uiState.value.targetUser
        val currentUser = _uiState.value.currentUser
        val selectedPackage = _uiState.value.selectedApp
        
        if (targetUser == null || currentUser == null) {
            _uiState.value = _uiState.value.copy(error = "User profile not loaded. Please try again.")
            return
        }
        
        if (targetUser.upiId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(showUpiMissingDialog = true)
            
            // Send a notification to the target user
            viewModelScope.launch {
                val notification = com.anish.owee.data.model.OweeNotification(
                    senderId = currentUser.id,
                    receiverId = targetUser.id,
                    type = "upi_alert",
                    title = "UPI ID Missing",
                    body = "${currentUser.displayName} tried to pay you. Add your UPI ID to receive it."
                )
                notificationRepository.sendNotification(notification)
            }
        } else if (selectedPackage != null) {
            // Save pending payment
            val amountStr = _uiState.value.amount
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val pendingPayment = PendingPayment(
                amount = amount,
                recipientId = targetUser.id,
                recipientName = targetUser.displayName ?: "Friend",
                sourceType = "CUSTOM",
                sourceId = targetUser.id,
                timestamp = System.currentTimeMillis()
            )
            PreferenceManager(context).savePendingPayment(pendingPayment)
            PaymentReminderManager(context).scheduleReminders()

            UpiPaymentManager.copyUpiId(context, targetUser.upiId)
            UpiPaymentManager.launchUpiApp(context, selectedPackage)
            setPaymentInProgress(true)
        }
    }

    fun createSettlements() {
        val amountToPay = _uiState.value.amount.toDoubleOrNull() ?: return
        val currentUserId = groupRepository.getCurrentUserId() ?: return
        val targetUser = _uiState.value.targetUser ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                var remainingAmount = amountToPay
                
                // Only consider sources where I OWE money
                val debtsToSettle = _uiState.value.sources.filter { it.amount < -0.01 }
                
                for (debt in debtsToSettle) {
                    if (remainingAmount <= 0.001) break
                    
                    val debtValue = kotlin.math.abs(debt.amount)
                    val rawSettlementValue = kotlin.math.min(remainingAmount, debtValue)
                    val settlementValue = kotlin.math.round(rawSettlementValue * 100.0) / 100.0
                    
                    if (settlementValue > 0) {
                        settlementRepository.createSettlement(
                            sourceType = debt.sourceType,
                            sourceId = debt.sourceId,
                            payerId = currentUserId,
                            receiverId = targetUser.id,
                            amount = settlementValue
                        )
                    }
                    
                    remainingAmount -= settlementValue
                }

                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                
                // Clear pending payment
                PreferenceManager(getApplication()).clearPendingPayment()
                PaymentReminderManager(getApplication()).cancelReminders()

                // Send ONE combined notification for the total amount
                viewModelScope.launch {
                    try {
                        val notification = com.anish.owee.data.model.OweeNotification(
                            senderId = currentUserId,
                            receiverId = targetUser.id,
                            type = "settlement",
                            title = "Payment Received",
                            body = "${_uiState.value.currentUser?.displayName ?: "Someone"} settled ₹${String.format(Locale.US, "%.2f", amountToPay)}",
                            data = buildJsonObject {
                                put("type", "settlement")
                                put("payer_name", _uiState.value.currentUser?.displayName ?: "Someone")
                                put("amount", amountToPay)
                                _uiState.value.currentUser?.photoUrl?.let { put("sender_photo", it) }
                            }
                        )
                        notificationRepository.sendNotification(notification)
                    } catch (e: Exception) {
                        android.util.Log.e("OWEE_NOTIFICATION", "Failed to send bulk settlement notification", e)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun sendReminder() {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: 0.0
        val targetUserId = _uiState.value.targetUser?.id ?: return
        val currentUserId = groupRepository.getCurrentUserId() ?: return
        val currentUserName = _uiState.value.currentUser?.displayName ?: "Someone"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val notification = com.anish.owee.data.model.OweeNotification(
                    senderId = currentUserId,
                    receiverId = targetUserId,
                    type = "reminder",
                    title = "Payment Reminder",
                    body = "$currentUserName is reminding you about ₹${String.format(Locale.US, "%.2f", amount)}"
                )
                val result = notificationRepository.sendNotification(notification)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to send reminder"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
