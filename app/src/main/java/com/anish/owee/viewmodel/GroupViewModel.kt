package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.GroupRepository
import com.anish.owee.data.repository.GroupRepositoryImpl
import com.anish.owee.viewmodel.state.GroupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
class GroupViewModel : ViewModel() {

    private val groupRepository: GroupRepository =
        GroupRepositoryImpl()

    private val _uiState =
        MutableStateFlow(GroupUiState())

    val uiState: StateFlow<GroupUiState> =
        _uiState.asStateFlow()

    init {
        loadGroups()
        observeGroupChanges()
    }
    fun loadGroups() {
        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val groups =
                    groupRepository.getGroups()

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        groups = groups
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
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

            val result =
                groupRepository.createGroup(
                    name = name,
                    memberIds = memberIds
                )

            result.onSuccess {

                loadGroups()

                onSuccess()
            }

            result.onFailure {

                _uiState.value =
                    _uiState.value.copy(
                        error = it.message
                    )
            }
        }
    }

    private fun observeGroupChanges() {

        viewModelScope.launch {

            groupRepository.groupChanges()
                .collectLatest {

                    android.util.Log.d(
                        "OWEE_GROUP",
                        "Group change received"
                    )

                    loadGroups()
                }
        }
    }
}