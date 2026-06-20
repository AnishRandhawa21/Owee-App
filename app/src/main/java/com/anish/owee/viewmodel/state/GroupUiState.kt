package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.Group

data class GroupUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)