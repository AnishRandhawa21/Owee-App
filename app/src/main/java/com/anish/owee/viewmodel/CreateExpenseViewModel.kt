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

        if (selected.contains(userId)) {
            selected.remove(userId)
        } else {
            selected.add(userId)
        }

        _uiState.value =
            _uiState.value.copy(
                selectedParticipantIds = selected
            )
    }

    fun createExpense(
        groupId: String
    ) {

        val amount =
            _uiState.value.amount.toDoubleOrNull()
                ?: return

        val currentUser =
            groupRepository.getCurrentUserId()
                ?: return

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true
                )

            val result =
                expenseRepository.createExpense(
                    groupId = groupId,
                    title = _uiState.value.title,
                    amount = amount,
                    payerId = currentUser,
                    participantIds =
                        _uiState.value.selectedParticipantIds.toList()
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