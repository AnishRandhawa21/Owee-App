package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.ExpenseRepository
import com.anish.owee.data.repository.ExpenseRepositoryImpl
import com.anish.owee.data.repository.GroupRepository
import com.anish.owee.data.repository.GroupRepositoryImpl
import com.anish.owee.viewmodel.state.CreateExpenseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateExpenseViewModel : ViewModel() {

    private val groupRepository: GroupRepository =
        GroupRepositoryImpl()

    private val expenseRepository: ExpenseRepository =
        ExpenseRepositoryImpl()

    private val _uiState =
        MutableStateFlow(CreateExpenseUiState())

    val uiState: StateFlow<CreateExpenseUiState> =
        _uiState.asStateFlow()

    fun loadMembers(groupId: String) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true
                )

            try {

                val members =
                    groupRepository.getGroupMembers(groupId)

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        members = members
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
            }
        }
    }

    fun updateTitle(title: String) {

        _uiState.value =
            _uiState.value.copy(
                title = title
            )
    }

    fun updateAmount(amount: String) {

        _uiState.value =
            _uiState.value.copy(
                amount = amount
            )
    }

    fun toggleParticipant(userId: String) {

        val selected =
            _uiState.value.selectedParticipantIds.toMutableSet()

        val customAmounts = _uiState.value.customAmounts.toMutableMap()

        if (selected.contains(userId)) {
            selected.remove(userId)
            customAmounts.remove(userId)
        } else {
            selected.add(userId)
        }

        _uiState.value =
            _uiState.value.copy(
                selectedParticipantIds = selected,
                customAmounts = customAmounts
            )
    }

    fun updateSplitMode(isCustom: Boolean) {
        val selected = _uiState.value.selectedParticipantIds
        
        // When switching to custom split, ensure everyone is "selected" to show textboxes
        val newSelected = if (isCustom) {
            _uiState.value.members.map { it.id }.toSet()
        } else {
            selected
        }

        _uiState.value = _uiState.value.copy(
            isCustomSplit = isCustom,
            selectedParticipantIds = newSelected
        )
    }

    fun updateCustomAmount(userId: String, amount: String) {
        val customAmounts = _uiState.value.customAmounts.toMutableMap()
        customAmounts[userId] = amount
        _uiState.value = _uiState.value.copy(customAmounts = customAmounts)
    }

    fun createExpense(
        groupId: String
    ) {

        val totalAmount =
            _uiState.value.amount.toDoubleOrNull()
                ?: return

        val currentUser =
            groupRepository.getCurrentUserId()
                ?: return

        val selectedIds = _uiState.value.selectedParticipantIds
        if (selectedIds.isEmpty()) return

        val participantShares = mutableMapOf<String, Double>()

        if (_uiState.value.isCustomSplit) {
            // Custom Split
            var sum = 0.0
            selectedIds.forEach { id ->
                val share = _uiState.value.customAmounts[id]?.toDoubleOrNull() ?: 0.0
                participantShares[id] = share
                sum += share
            }

            if (kotlin.math.abs(sum - totalAmount) > 0.01) {
                _uiState.value = _uiState.value.copy(
                    error = "Total split (₹$sum) doesn't match expense amount (₹$totalAmount)"
                )
                return
            }
        } else {
            // Equal Split
            val share = totalAmount / selectedIds.size
            selectedIds.forEach { id ->
                participantShares[id] = share
            }
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            val result =
                expenseRepository.createExpense(
                    groupId = groupId,
                    title = _uiState.value.title,
                    amount = totalAmount,
                    payerId = currentUser,
                    participants = participantShares
                )

            result.onSuccess {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
            }

            result.onFailure {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = it.message
                    )
            }
        }
    }
}