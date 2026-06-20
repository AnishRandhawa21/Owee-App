package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.GroupMemberBalance
import com.anish.owee.data.model.User

data class GroupDetailUiState(

    val members: List<User> = emptyList(),

    val expenses: List<Expense> = emptyList(),

    val balances: List<GroupMemberBalance> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null
)