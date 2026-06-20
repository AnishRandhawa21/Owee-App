package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.Group
import com.anish.owee.data.model.GroupMemberBalance
import com.anish.owee.data.model.User
import com.anish.owee.data.model.ExpenseParticipant

data class GroupDetailUiState(

    val group: Group? = null,

    val members: List<User> = emptyList(),

    val expenses: List<Expense> = emptyList(),

    val balances: List<GroupMemberBalance> = emptyList(),

    val participantsByExpense:
    Map<String, List<ExpenseParticipant>> = emptyMap(),

    val isLoading: Boolean = false,

    val error: String? = null
)