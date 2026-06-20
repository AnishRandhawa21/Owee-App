package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.User

data class CreateExpenseUiState(

    val isLoading: Boolean = false,

    val members: List<User> = emptyList(),

    val title: String = "",

    val amount: String = "",

    val selectedParticipantIds: Set<String> = emptySet(),

    val error: String? = null,

    val isSuccess: Boolean = false
)