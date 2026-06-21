package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
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
// ... rest of the fetch logic ...

                val members =
                    groupRepository.getGroupMembers(groupId)

                val expenses =
                    expenseRepository.getGroupExpenses(groupId)

                val settlements =
                    settlementRepository.getSettlements(
                        sourceType = "GROUP",
                        sourceId = groupId
                    )

                val allParticipants =
                    expenseRepository.getAllExpenseParticipants(
                        expenses.map { it.id }
                    )

                val participantsByExpense =
                    allParticipants.groupBy {
                        it.expenseId
                    }

                val balances =
                    GroupBalanceCalculator
                        .calculateBalances(
                            currentUserId = currentUserId,
                            expenses = expenses,
                            participantsByExpense = participantsByExpense,
                            settlements = settlements
                        )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        group = group,
                        members = members,
                        expenses = expenses,
                        settlements = settlements,
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
}