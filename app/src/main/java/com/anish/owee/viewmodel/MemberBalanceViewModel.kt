package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.ExpenseParticipant
import com.anish.owee.data.model.MemberTransaction
import com.anish.owee.data.model.Settlement
import com.anish.owee.data.repository.ExpenseRepository
import com.anish.owee.data.repository.ExpenseRepositoryImpl
import com.anish.owee.viewmodel.state.MemberBalanceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberBalanceViewModel : ViewModel() {
    private val _uiState =
        MutableStateFlow(MemberBalanceUiState())

    val uiState: StateFlow<MemberBalanceUiState> =
        _uiState.asStateFlow()

    fun loadTransactions(
        currentUserId: String,
        memberId: String,
        expenses: List<Expense>,
        participantsByExpense: Map<String, List<ExpenseParticipant>>,
        settlements: List<Settlement>
    ){

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true
                )

            val transactions =
                mutableListOf<MemberTransaction>()

            // PROCESS EXPENSES
            expenses.forEach { expense ->

                val participants =
                    participantsByExpense[
                        expense.id
                    ] ?: emptyList()

                if (expense.payerId == currentUserId) {

                    participants
                        .firstOrNull {
                            it.userId == memberId
                        }
                        ?.let {

                            transactions.add(
                                MemberTransaction(
                                    expenseId = expense.id,
                                    title = expense.title,
                                    amount = it.shareAmount,
                                    createdAt = expense.createdAt
                                )
                            )
                        }
                }

                else if (expense.payerId == memberId) {

                    participants
                        .firstOrNull {
                            it.userId == currentUserId
                        }
                        ?.let {

                            transactions.add(
                                MemberTransaction(
                                    expenseId = expense.id,
                                    title = expense.title,
                                    amount = -it.shareAmount,
                                    createdAt = expense.createdAt
                                )
                            )
                        }
                }
            }

            // PROCESS SETTLEMENTS
            settlements.forEach { settlement ->

                if (
                    settlement.payerId == currentUserId &&
                    settlement.receiverId == memberId
                ) {

                    transactions.add(
                        MemberTransaction(
                            expenseId = "settlement_${settlement.id}",
                            title = "Settlement (Paid)",
                            amount = settlement.amount,
                            createdAt = settlement.createdAt
                        )
                    )

                } else if (
                    settlement.payerId == memberId &&
                    settlement.receiverId == currentUserId
                ) {

                    transactions.add(
                        MemberTransaction(
                            expenseId = "settlement_${settlement.id}",
                            title = "Settlement (Received)",
                            amount = -settlement.amount,
                            createdAt = settlement.createdAt
                        )
                    )
                }
            }

            _uiState.value =
                _uiState.value.copy(
                    isLoading = false,
                    transactions = transactions.sortedByDescending { it.createdAt },
                    totalAmount = transactions.sumOf {
                        it.amount
                    }
                )
        }
    }
}