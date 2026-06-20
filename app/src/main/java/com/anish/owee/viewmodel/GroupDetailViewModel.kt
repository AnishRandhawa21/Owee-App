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

    fun loadCachedGroupData(groupId: String) {
        val cached = preferenceManager.getGroupDetail(groupId)
        if (cached != null) {
            _uiState.value = cached.copy(isLoading = false)
        }
    }

    fun loadGroupData(groupId: String) {

        viewModelScope.launch {

            if (_uiState.value.group == null) {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
            }

            try {

                val group =
                    groupRepository.getGroup(groupId)

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

                expenses.forEach { expense ->

                    android.util.Log.d(
                        "OWEE_EXPENSE_DEBUG",
                        "Expense=${expense.title} amount=${expense.amount} payer=${expense.payerId}"
                    )

                    val participants =
                        participantsByExpense[
                            expense.id
                        ] ?: emptyList()

                    participants.forEach {

                        android.util.Log.d(
                            "OWEE_EXPENSE_DEBUG",
                            "participant=${it.userId} share=${it.shareAmount}"
                        )
                    }
                }


                val currentUserId =
                    groupRepository.getCurrentUserId()
                        ?: return@launch



                val balances =
                    GroupBalanceCalculator
                        .calculateBalances(
                            currentUserId = currentUserId,
                            expenses = expenses,
                            participantsByExpense = participantsByExpense,
                            settlements = settlements
                        )
                balances.forEach {

                    android.util.Log.d(
                        "OWEE_BALANCE",
                        "${it.userId} -> ${it.amount}"
                    )

                    android.util.Log.d(
                        "OWEE_FINAL_BALANCE",
                        "${it.userId} -> ${it.amount}"
                    )
                }
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
                        error = e.message
                    )
            }
        }
    }
    fun getCurrentUserId(): String? {
        return groupRepository.getCurrentUserId()
    }
}