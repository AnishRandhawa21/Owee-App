package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.model.Settlement
import com.anish.owee.data.repository.ExpenseRepository
import com.anish.owee.data.repository.ExpenseRepositoryImpl
import com.anish.owee.data.repository.GroupRepository
import com.anish.owee.data.repository.GroupRepositoryImpl
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.viewmodel.state.GroupDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import com.anish.owee.data.repository.SettlementRepository
import com.anish.owee.data.repository.SettlementRepositoryImpl

class GroupDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val groupRepository: GroupRepository =
        GroupRepositoryImpl()

    private val expenseRepository: ExpenseRepository =
        ExpenseRepositoryImpl()

    private val settlementRepository: SettlementRepository =
        SettlementRepositoryImpl()

    private val preferenceManager = PreferenceManager(application)

    private val _uiState =
        MutableStateFlow(GroupDetailUiState())

    val uiState: StateFlow<GroupDetailUiState> =
        _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var observationJob: kotlinx.coroutines.Job? = null

    fun loadCachedGroupData(groupId: String) {
        if (currentGroupId == groupId) return
        currentGroupId = groupId
        
        val cached = preferenceManager.getGroupDetail(groupId)
        if (cached != null) {
            _uiState.value = cached.copy(isLoading = false)
        }
        observeChanges(groupId)
    }

    private fun observeChanges(groupId: String) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            com.anish.owee.data.remote.SupabaseProvider.ensureRealtimeConnected()
            merge(
                expenseRepository.expenseChanges(),
                settlementRepository.settlementChanges(),
                groupRepository.groupChanges()
            ).collectLatest {
                loadGroupData(groupId)
            }
        }
    }

    fun loadGroupData(groupId: String) {

        viewModelScope.launch {
            
            val currentUserId = groupRepository.getCurrentUserId()
            if (currentUserId == null) {
                // If we have data, just turn off loading. If not, show error.
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.group == null) "Session expired" else null
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // Fetch group first to ensure it exists
                val group = groupRepository.getGroup(groupId)
                    ?: throw Exception("Group not found")

                val members = groupRepository.getGroupMembers(groupId)
                val expenses = expenseRepository.getGroupExpenses(groupId)

                // 1. Fetch Group-specific allocations
                val allocations = settlementRepository.getAllocations(
                    sourceType = "GROUP",
                    sourceId = groupId
                )

                val allParticipants =
                    expenseRepository.getGroupExpenseParticipants(groupId)

                val participantsByExpense =
                    allParticipants.groupBy {
                        it.expenseId
                    }

                // 2. Use allocations instead of settlements
                val balances =
                    GroupBalanceCalculator
                        .calculateBalances(
                            currentUserId = currentUserId,
                            expenses = expenses,
                            participantsByExpense = participantsByExpense,
                            allocations = allocations
                        )

                // 3. Map Allocations to Settlement for UI Compatibility
                val settlementDisplayList = allocations.map { allocation ->
                    Settlement(
                        id = allocation.id,
                        sourceType = allocation.sourceType,
                        sourceId = allocation.sourceId,
                        payerId = allocation.payerId,
                        receiverId = allocation.receiverId,
                        amount = kotlin.math.abs(allocation.amount),
                        status = "completed",
                        createdAt = allocation.createdAt
                    )
                }

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        group = group,
                        members = members,
                        expenses = expenses,
                        settlements = settlementDisplayList,
                        balances = balances,
                        participantsByExpense =
                            participantsByExpense
                    )

                // Save to cache
                preferenceManager.saveGroupDetail(groupId, _uiState.value)

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = if (_uiState.value.group == null) e.message else null
                    )
            }
        }
    }
    fun getCurrentUserId(): String? {
        return groupRepository.getCurrentUserId()
    }

    fun deleteExpense(expenseId: String, groupId: String) {
        viewModelScope.launch {
            val currentUserId = groupRepository.getCurrentUserId() ?: return@launch
            val expense = _uiState.value.expenses.find { it.id == expenseId }
            
            if (expense?.payerId != currentUserId) {
                _uiState.value = _uiState.value.copy(error = "Only the payer can delete this expense")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            expenseRepository.deleteExpense(expenseId).onSuccess {
                loadGroupData(groupId)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun deleteSettlement(settlementId: String, groupId: String) {
        viewModelScope.launch {
            val currentUserId = groupRepository.getCurrentUserId() ?: return@launch
            val settlement = _uiState.value.settlements.find { it.id == settlementId }

            if (settlement?.payerId != currentUserId) {
                _uiState.value = _uiState.value.copy(error = "Only the payer can delete this settlement")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            settlementRepository.deleteSettlement(settlementId).onSuccess {
                loadGroupData(groupId)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }
}
