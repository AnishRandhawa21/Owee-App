package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.repository.*
import com.anish.owee.domain.GroupBalanceCalculator
import com.anish.owee.viewmodel.state.GroupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val groupRepository: GroupRepository =
        GroupRepositoryImpl()

    private val expenseRepository: ExpenseRepository =
        ExpenseRepositoryImpl()

    private val settlementRepository: SettlementRepository =
        SettlementRepositoryImpl()

    private val preferenceManager = PreferenceManager(application)

    private val _uiState =
        MutableStateFlow(GroupUiState())

    val uiState: StateFlow<GroupUiState> =
        _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    init {
        loadCachedGroups()
        loadGroups()
        observeGroupChanges()
    }

    private fun loadCachedGroups() {
        val cached = preferenceManager.getGroups()
        if (cached.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                groups = cached,
                isLoading = false
            )
        }
    }

    fun loadGroups(isSilent: Boolean = false) {
        viewModelScope.launch {

            if (!isSilent && _uiState.value.groups.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val groups = groupRepository.getGroupsWithMetadata()
                val currentUserId = groupRepository.getCurrentUserId()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groups = groups,
                    currentUserId = currentUserId
                )

                // Save to cache
                preferenceManager.saveGroups(groups)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun createGroup(
        name: String,
        memberIds: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = groupRepository.createGroup(
                name = name,
                memberIds = memberIds
            )

            result.onSuccess {
                loadGroups(isSilent = true) // Update list silently after creation
                onSuccess()
            }

            result.onFailure {
                _uiState.value = _uiState.value.copy(
                    error = it.message
                )
            }
        }
    }

    fun validateGroupDeletion(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, validationAllSettled = null)

            try {
                val currentUserId = groupRepository.getCurrentUserId() ?: return@launch

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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationAllSettled = balances.isEmpty()
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = groupRepository.deleteGroup(groupId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groups = _uiState.value.groups.filter { it.group.id != groupId },
                    validationAllSettled = null
                )
            }

            result.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not delete group. Please try again."
                )
            }
        }
    }

    fun resetValidation() {
        _uiState.value = _uiState.value.copy(validationAllSettled = null)
    }

    private fun observeGroupChanges() {

        viewModelScope.launch {
            com.anish.owee.data.remote.SupabaseProvider.ensureRealtimeConnected()
            groupRepository.groupChanges()
                .collectLatest {

                    android.util.Log.d(
                        "OWEE_REALTIME",
                        "Group change received"
                    )

                    loadGroups()
                }
        }
    }
}