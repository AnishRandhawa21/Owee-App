package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.model.PendingPayment
import com.anish.owee.data.repository.*
import com.anish.owee.domain.FriendBalanceCalculator
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.navigation.Route
import com.anish.owee.utils.PaymentReminderManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

sealed class PendingPaymentEvent {
    data class Navigate(val route: String) : PendingPaymentEvent()
}

class PendingPaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val preferenceManager = PreferenceManager(application)
    private val reminderManager = PaymentReminderManager(application)
    private val settlementRepository: SettlementRepository = SettlementRepositoryImpl()
    private val authRepository: AuthRepository = AuthRepositoryImpl()
    private val groupRepository: GroupRepository = GroupRepositoryImpl()
    private val expenseRepository: ExpenseRepository = ExpenseRepositoryImpl()
    private val friendshipRepository: FriendshipRepository = FriendshipRepositoryImpl()
    private val friendRequestRepository: FriendRequestRepository = FriendRequestRepositoryImpl()

    private val _pendingPayment = MutableStateFlow<PendingPayment?>(null)
    val pendingPayment = _pendingPayment.asStateFlow()

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming = _isConfirming.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<PendingPaymentEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun checkPendingPayment() {
        if (_isConfirming.value || _isSuccess.value) return 
        _pendingPayment.value = preferenceManager.getPendingPayment()
    }

    fun confirmPayment() {
        if (_isConfirming.value || _isSuccess.value) return 
        val payment = _pendingPayment.value ?: return
        
        _isConfirming.value = true
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser() ?: run {
                    _isConfirming.value = false
                    return@launch
                }
                
                // 1. Trigger navigation instantly before heavy background work
                val route = when(payment.sourceType) {
                    "FRIEND" -> "${Route.FriendDetail.route}/${payment.recipientId}"
                    "GROUP" -> "${Route.GroupDetail.route}/${payment.sourceId}"
                    else -> Route.Home.route 
                }

                _isSuccess.value = true
                _navigationEvent.emit(PendingPaymentEvent.Navigate(route))
                
                // 2. Clear from UI immediately so the sheet disappears
                _pendingPayment.value = null

                // 3. Perform the actual database work in the background
                if (payment.sourceType == "CUSTOM") {
                    performBulkSettlement(payment, currentUser.id)
                } else {
                    settlementRepository.createSettlement(
                        sourceType = payment.sourceType,
                        sourceId = payment.sourceId,
                        payerId = currentUser.id,
                        receiverId = payment.recipientId,
                        amount = payment.amount
                    )
                }

                // Clear storage and reminders
                preferenceManager.clearPendingPayment()
                reminderManager.cancelReminders()

            } catch (e: Exception) {
                android.util.Log.e("PendingPayment", "Payment confirmation failed", e)
                clearPending() 
            } finally {
                _isConfirming.value = false
                _isSuccess.value = false
            }
        }
    }

    private suspend fun performBulkSettlement(payment: PendingPayment, currentUserId: String) {
        try {
            coroutineScope {
                val targetUserId = payment.recipientId
                val groups = groupRepository.getGroupsWithMetadata()
                
                val groupSources = groups.map { groupMetadata ->
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
                            if (gb.amount < -0.01) {
                                DebtInfo("GROUP", groupId, gb.amount)
                            } else null
                        }
                    }
                }.awaitAll().filterNotNull()

                val friendship = friendshipRepository.getFriendships().firstOrNull {
                    it.senderId == targetUserId || it.receiverId == targetUserId
                }

                val friendSource = if (friendship != null) {
                    val requests = friendRequestRepository.getRequestsForFriend(targetUserId)
                    val settlements = settlementRepository.getSettlements("FRIEND", friendship.id)
                    val friendNet = FriendBalanceCalculator.calculate(currentUserId, requests, settlements)
                    if (friendNet < -0.01) DebtInfo("FRIEND", friendship.id, friendNet) else null
                } else null

                val allDebts = mutableListOf<DebtInfo>()
                allDebts.addAll(groupSources)
                friendSource?.let { allDebts.add(it) }

                var remainingAmount = payment.amount
                // Simplified sorting: settle largest debts first
                for (debt in allDebts.sortedBy { it.amount }) { 
                    if (remainingAmount <= 0.001) break
                    val debtValue = abs(debt.amount)
                    val settlementValue = round(min(remainingAmount, debtValue) * 100.0) / 100.0
                    if (settlementValue > 0) {
                        settlementRepository.createSettlement(debt.sourceType, debt.sourceId, currentUserId, targetUserId, settlementValue)
                        remainingAmount -= settlementValue
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PendingPayment", "Bulk settlement failed", e)
        }
    }

    private data class DebtInfo(val sourceType: String, val sourceId: String, val amount: Double)

    fun cancelPayment() {
        clearPending()
    }

    private fun clearPending() {
        preferenceManager.clearPendingPayment()
        reminderManager.cancelReminders()
        _pendingPayment.value = null
    }
}
