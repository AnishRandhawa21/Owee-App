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

    private val deletedGroupIds = mutableSetOf<String>()

    fun loadGroups(isSilent: Boolean = false) {
        viewModelScope.launch {
            val cachedGroups = preferenceManager.getGroups()
            if (cachedGroups.isNotEmpty() && _uiState.value.groups.isEmpty()) {
                _uiState.value = _uiState.value.copy(groups = cachedGroups)
            }

            if (!isSilent && _uiState.value.groups.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            try {
                val currentUserId = groupRepository.getCurrentUserId() ?: return@launch
                val rawNetworkGroups = groupRepository.getGroupsWithMetadata()

                // Filter out any groups that we just deleted optimistically
                val networkGroups = rawNetworkGroups.filter { !deletedGroupIds.contains(it.group.id) }

                if (networkGroups.isNotEmpty()) {
                    // UPDATE UI IMMEDIATELY WITH NETWORK DATA
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        groups = networkGroups.map { net ->
                            // Preserve the settled status from state if it already exists to avoid badge flicker
                            val existing = _uiState.value.groups.find { it.group.id == net.group.id }
                            net.copy(isSettled = existing?.isSettled ?: true)
                        },
                        currentUserId = currentUserId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, currentUserId = currentUserId)
                }

                // BACKGROUND: Update settlement badges
                try {
                    val updatedGroups = networkGroups.map { metadata ->
                        val groupId = metadata.group.id
                        val expenses = expenseRepository.getGroupExpenses(groupId)

                        val isSettled = if (expenses.isEmpty()) {
                            true
                        } else {
                            val allocations = settlementRepository.getAllocations("GROUP", groupId)
                            val allParticipants = expenseRepository.getAllExpenseParticipants(expenses.map { it.id })
                            val participantsByExpense = allParticipants.groupBy { it.expenseId }

                            val balances = GroupBalanceCalculator.calculateBalances(
                                currentUserId = currentUserId,
                                expenses = expenses,
                                participantsByExpense = participantsByExpense,
                                allocations = allocations
                            )
                            balances.isEmpty()
                        }
                        metadata.copy(isSettled = isSettled)
                    }

                    // Final safety check: ensure we don't bring back deleted groups
                    val finalGroups = updatedGroups.filter { !deletedGroupIds.contains(it.group.id) }

                    _uiState.value = _uiState.value.copy(groups = finalGroups)
                    preferenceManager.saveGroups(finalGroups)
                } catch (_: Exception) {
                    // Fail silently for background updates
                }

            } catch (e: Exception) {
                // If we have cached data, don't show the technical error
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.groups.isEmpty()) "Unable to connect to server. Please check your internet connection." else null
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
                val allocations = settlementRepository.getAllocations("GROUP", groupId)
                val allParticipants = expenseRepository.getAllExpenseParticipants(expenses.map { it.id })
                val participantsByExpense = allParticipants.groupBy { it.expenseId }

                val balances = GroupBalanceCalculator.calculateBalances(
                    currentUserId = currentUserId,
                    expenses = expenses,
                    participantsByExpense = participantsByExpense,
                    allocations = allocations
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
            // OPTIMISTIC DELETE: Update UI instantly
            deletedGroupIds.add(groupId)
            val previousGroups = _uiState.value.groups
            val updatedGroups = previousGroups.filter { it.group.id != groupId }
            
            _uiState.value = _uiState.value.copy(
                groups = updatedGroups,
                validationAllSettled = null
            )
            
            // Update cache instantly
            preferenceManager.saveGroups(updatedGroups)

            val result = groupRepository.deleteGroup(groupId)

            result.onFailure {
                // ROLLBACK: If it actually fails on the server
                deletedGroupIds.remove(groupId)
                _uiState.value = _uiState.value.copy(
                    groups = previousGroups,
                    error = "Could not delete group. Please try again."
                )
                preferenceManager.saveGroups(previousGroups)
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