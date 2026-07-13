package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.model.PendingPayment
import com.anish.owee.data.repository.*
import com.anish.owee.domain.FriendBalanceCalculator
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.domain.SettlementPlanner
import com.anish.owee.domain.SettlementSource
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
                    val balanceSource = SettlementSource(
                        sourceType = payment.sourceType,
                        sourceId = payment.sourceId,
                        amount = -payment.amount,
                        createdAt = ""
                    )
                    val plan = SettlementPlanner.plan(
                        currentUserId = currentUser.id,
                        targetUserId = payment.recipientId,
                        cashAmount = payment.amount,
                        sources = listOf(balanceSource),
                        sessionType = payment.sourceType
                    )
                    settlementRepository.createSettlementSession(plan)
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
                
                val sourceResults = groups.map { groupMetadata ->
                    async {
                        val groupId = groupMetadata.group.id
                        val expenses = expenseRepository.getGroupExpenses(groupId)
                        val allocations = settlementRepository.getAllocations("GROUP", groupId)
                        val allParticipants = expenseRepository.getGroupExpenseParticipants(groupId)
                        val participantsByExpense = allParticipants.groupBy { it.expenseId }

                        val groupBalances = GroupBalanceCalculator.calculateBalances(
                            currentUserId = currentUserId,
                            expenses = expenses,
                            participantsByExpense = participantsByExpense,
                            allocations = allocations
                        )

                        groupBalances.find { it.userId == targetUserId }?.let { gb ->
                            SettlementSource(
                                sourceType = "GROUP",
                                sourceId = groupId,
                                amount = gb.amount,
                                createdAt = groupMetadata.group.createdAt
                            )
                        }
                    }
                }.awaitAll().filterNotNull().toMutableList()

                val friendship = friendshipRepository.getFriendships().firstOrNull {
                    it.senderId == targetUserId || it.receiverId == targetUserId
                }

                if (friendship != null) {
                    val requests = friendRequestRepository.getRequestsForFriend(targetUserId)
                    val allocations = settlementRepository.getAllocations("FRIEND", friendship.id)
                    val friendNet = FriendBalanceCalculator.calculate(currentUserId, requests, allocations)
                    sourceResults.add(
                        SettlementSource(
                            sourceType = "FRIEND",
                            sourceId = friendship.id,
                            amount = friendNet,
                            createdAt = friendship.createdAt
                        )
                    )
                }

                val plan = SettlementPlanner.plan(
                    currentUserId = currentUserId,
                    targetUserId = targetUserId,
                    cashAmount = payment.amount,
                    sources = sourceResults,
                    sessionType = "HOME"
                )

                settlementRepository.createSettlementSession(plan)
            }
        } catch (e: Exception) {
            android.util.Log.e("PendingPayment", "Bulk settlement failed", e)
        }
    }

    fun cancelPayment() {
        clearPending()
    }

    private fun clearPending() {
        preferenceManager.clearPendingPayment()
        reminderManager.cancelReminders()
        _pendingPayment.value = null
    }
}
