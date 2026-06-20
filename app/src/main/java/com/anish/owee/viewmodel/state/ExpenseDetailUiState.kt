package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.ExpenseParticipantUser

data class ExpenseDetailUiState(

    val participants: List<ExpenseParticipantUser> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null
)