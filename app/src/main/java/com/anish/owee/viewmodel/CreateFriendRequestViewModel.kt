package com.anish.owee.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.anish.owee.data.repository.FriendRequestRepository
import com.anish.owee.data.repository.FriendRequestRepositoryImpl
import com.anish.owee.viewmodel.state.CreateFriendRequestUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
class CreateFriendRequestViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow(CreateFriendRequestUiState())

    val uiState: StateFlow<CreateFriendRequestUiState> =
        _uiState.asStateFlow()

    fun updateAmount(amount: String) {
        _uiState.value =
            _uiState.value.copy(
                amount = amount
            )
    }

    fun updateNote(note: String) {
        _uiState.value =
            _uiState.value.copy(
                note = note
            )
    }

    private val repository: FriendRequestRepository =
        FriendRequestRepositoryImpl()

    fun createRequest(friendId: String) {

        val amount =
            uiState.value.amount.toDoubleOrNull()

        if (amount == null) return

        viewModelScope.launch {

            val result = repository.createRequest(
                friendId = friendId,
                amount = amount,
                note = uiState.value.note.ifBlank { null }
            )

        }
    }

}