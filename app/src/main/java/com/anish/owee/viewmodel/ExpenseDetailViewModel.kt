package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.ExpenseRepository
import com.anish.owee.data.repository.ExpenseRepositoryImpl
import com.anish.owee.viewmodel.state.ExpenseDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseDetailViewModel : ViewModel() {

    private val repository: ExpenseRepository =
        ExpenseRepositoryImpl()

    private val _uiState =
        MutableStateFlow(ExpenseDetailUiState())

    val uiState: StateFlow<ExpenseDetailUiState> =
        _uiState.asStateFlow()

    fun loadExpenseParticipants(
        expenseId: String
    ) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true
                )

            try {

                val participants =
                    repository.getExpenseParticipants(
                        expenseId
                    )

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        participants = participants
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
}