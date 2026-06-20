package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class GroupDetailUiState(

    val group: Group? = null,

    val members: List<User> = emptyList(),

    val expenses: List<Expense> = emptyList(),

    val settlements: List<Settlement> = emptyList(),

    val balances: List<GroupMemberBalance> = emptyList(),

    val participantsByExpense: Map<String, List<ExpenseParticipant>> = emptyMap(),

    @Transient
    val isLoading: Boolean = false,

    @Transient
    val error: String? = null
)