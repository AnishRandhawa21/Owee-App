package com.anish.owee.viewmodel.state

import com.anish.owee.data.model.Group
import com.anish.owee.data.model.User
import kotlinx.serialization.Serializable

data class GroupUiState(
    val groups: List<GroupWithMetadata> = emptyList(),
    val currentUserId: String? = null,
    val validationAllSettled: Boolean? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@Serializable
data class GroupWithMetadata(
    val group: Group,
    val creator: User? = null,
    val members: List<User> = emptyList()
)
