package com.anish.owee.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.local.PreferenceManager
import com.anish.owee.data.repository.GroupRepository
import com.anish.owee.data.repository.GroupRepositoryImpl
import com.anish.owee.viewmodel.state.GroupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class GroupViewModel(application: Application) : AndroidViewModel(application) {

    private val groupRepository: GroupRepository =
        GroupRepositoryImpl()

    private val preferenceManager = PreferenceManager(application)

    private val _uiState =
        MutableStateFlow(GroupUiState())

    val uiState: StateFlow<GroupUiState> =
        _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    init {
        loadCachedGroups()
        loadGroups()
        observeGroupChanges()
    }

    private fun loadCachedGroups() {
        val cached = preferenceManager.getGroups()
        if (cached.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                groups = cached,
                isLoading = false
            )
        }
    }

    fun loadGroups(isSilent: Boolean = false) {
        viewModelScope.launch {

            if (!isSilent && _uiState.value.groups.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
            }

            try {
                val groups = groupRepository.getGroupsWithMetadata()
                val currentUserId = groupRepository.getCurrentUserId()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    groups = groups,
                    currentUserId = currentUserId
                )

                // Save to cache
                preferenceManager.saveGroups(groups)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun createGroup(
        name: String,
        memberIds: List<String>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = groupRepository.createGroup(
                name = name,
                memberIds = memberIds
            )

            result.onSuccess {
                loadGroups(isSilent = true) // Update list silently after creation
                onSuccess()
            }

            result.onFailure {
                _uiState.value = _uiState.value.copy(
                    error = it.message
                )
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            // Optimistic UI: Remove the group instantly from the local state
            val previousState = _uiState.value
            _uiState.value = _uiState.value.copy(
                groups = previousState.groups.filter { it.group.id != groupId }
            )

            val result = groupRepository.deleteGroup(groupId)
            
            result.onFailure {
                // Rollback: If backend fails, put the group back and show error
                _uiState.value = previousState.copy(
                    error = "Could not delete group. Please try again."
                )
            }
            // No need to call loadGroups() on success because we already updated the state!
        }
    }

    private fun observeGroupChanges() {

        viewModelScope.launch {
            com.anish.owee.data.remote.SupabaseProvider.ensureRealtimeConnected()
            groupRepository.groupChanges()
                .collectLatest {

                    android.util.Log.d(
                        "OWEE_REALTIME",
                        "Group change received"
                    )

                    loadGroups()
                }
        }
    }
}