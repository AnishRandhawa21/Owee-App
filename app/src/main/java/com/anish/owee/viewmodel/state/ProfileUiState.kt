package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.User

data class ProfileUiState(

    val isLoading: Boolean = false,

    val isSaving: Boolean = false,

    val user: User? = null,

    val upiId: String = "",

    val error: String? = null,

    val saveSuccess: Boolean = false
)