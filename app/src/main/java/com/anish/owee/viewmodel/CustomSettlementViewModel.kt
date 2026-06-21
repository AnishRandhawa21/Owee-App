package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.model.User
import com.anish.owee.data.repository.*
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.viewmodel.state.DebtSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class CustomSettlementUiState(
    val isLoading: Boolean = false,
    val targetUser: User? = null,
    val totalDebt: Double = 0.0,
    val amount: String = "",
    val sources: List<DebtSource> = emptyList(),
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isPaymentInProgress: Boolean = false,
    val showConfirmationDialog: Boolean = false
)

class CustomSettlementViewModel(application: Application) : AndroidViewModel(application) {

    private val groupRepository: GroupRepository = GroupRepositoryImpl()
    private val expenseRepository: ExpenseRepository = ExpenseRepositoryImpl()
    private val settlementRepository: SettlementRepository = SettlementRepositoryImpl()
    private val friendshipRepository: FriendshipRepository = FriendshipRepositoryImpl()
    private val friendRequestRepository: FriendRequestRepository = FriendRequestRepositoryImpl()
    private val authRepository: AuthRepository = AuthRepositoryImpl()

    private val _uiState = MutableStateFlow(CustomSettlementUiState())
    val uiState: StateFlow<CustomSettlementUiState> = _uiState.asStateFlow()

    fun loadUserDebts(targetUserId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                coroutineScope {
                    val currentUserId = groupRepository.getCurrentUserId() ?: return@coroutineScope
                    val userDeferred = async { authRepository.getUserById(targetUserId) }
                    val groupsDeferred = async { groupRepository.getGroupsWithMetadata() }
                    
                    val groups = groupsDeferred.await()
                    val user = userDeferred.await()

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
                        val friendship = friendshipRepository.getAcceptedFriendships().firstOrNull {
                            it.senderId == targetUserId || it.receiverId == targetUserId
                        }

                        if (friendship != null) {
                            val requests = friendRequestRepository.getRequestsForFriend(targetUserId)
                            val settlements = settlementRepository.getSettlements("FRIEND", friendship.id)

                            var totalRequestedByMe = 0.0
                            var totalRequestedByFriend = 0.0
                            requests.forEach { if (it.creatorId == currentUserId) totalRequestedByMe += it.amount else totalRequestedByFriend += it.amount }

                            var totalPaidByMe = 0.0
                            var totalReceivedByMe = 0.0
                            settlements.forEach { if (it.payerId == currentUserId) totalPaidByMe += it.amount else totalReceivedByMe += it.amount }

                            val friendNet = (totalRequestedByMe - totalRequestedByFriend) + (totalPaidByMe - totalReceivedByMe)
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
                    val settlementValue = kotlin.math.min(remainingAmount, debtValue)
                    
                    settlementRepository.createSettlement(
                        sourceType = debt.sourceType,
                        sourceId = debt.sourceId,
                        payerId = currentUserId,
                        receiverId = targetUser.id,
                        amount = settlementValue
                    )
                    
                    remainingAmount -= settlementValue
                }

                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}