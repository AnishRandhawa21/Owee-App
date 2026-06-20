package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.*
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.viewmodel.state.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val groupRepository: GroupRepository = GroupRepositoryImpl()
    private val expenseRepository: ExpenseRepository = ExpenseRepositoryImpl()
    private val settlementRepository: SettlementRepository = SettlementRepositoryImpl()
    private val friendshipRepository: FriendshipRepository = FriendshipRepositoryImpl()
    private val friendRequestRepository: FriendRequestRepository = FriendRequestRepositoryImpl()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val currentUserId = groupRepository.getCurrentUserId() ?: return@launch

                // 1. CALCULATE GROUP BALANCES
                val groups = groupRepository.getGroupsWithMetadata()
                var totalGroupBalance = 0.0

                groups.forEach { groupMetadata ->
                    val groupId = groupMetadata.group.id
                    val expenses = expenseRepository.getGroupExpenses(groupId)
                    val settlements = settlementRepository.getSettlements("GROUP", groupId)
                    val allParticipants = expenseRepository.getAllExpenseParticipants(expenses.map { it.id })
                    val participantsByExpense = allParticipants.groupBy { it.expenseId }

                    val balances = GroupBalanceCalculator.calculateBalances(
                        currentUserId = currentUserId,
                        expenses = expenses,
                        participantsByExpense = participantsByExpense,
                        settlements = settlements
                    )
                    
                    totalGroupBalance += balances.sumOf { it.amount }
                }

                // 2. CALCULATE FRIEND BALANCES
                // We'll need a way to get all requests and settlements for friends.
                // Since repositories are limited, we fetch accepted friends and then their requests.
                val friends = friendshipRepository.getAcceptedFriendships()
                var totalFriendBalance = 0.0

                friends.forEach { friendship ->
                    val friendId = if (friendship.senderId == currentUserId) friendship.receiverId else friendship.senderId
                    
                    val requests = friendRequestRepository.getRequestsForFriend(friendId)
                    val settlements = settlementRepository.getSettlements("FRIEND", friendship.id)

                    var totalRequestedByMe = 0.0
                    var totalRequestedByFriend = 0.0
                    requests.forEach { 
                        if (it.creatorId == currentUserId) totalRequestedByMe += it.amount 
                        else totalRequestedByFriend += it.amount 
                    }

                    var totalPaidByMe = 0.0
                    var totalReceivedByMe = 0.0
                    settlements.forEach { 
                        if (it.payerId == currentUserId) totalPaidByMe += it.amount 
                        else totalReceivedByMe += it.amount 
                    }

                    val friendNet = (totalRequestedByMe - totalRequestedByFriend) + (totalPaidByMe - totalReceivedByMe)
                    totalFriendBalance += friendNet
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groupBalance = totalGroupBalance,
                    friendBalance = totalFriendBalance,
                    totalBalance = totalGroupBalance + totalFriendBalance
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}