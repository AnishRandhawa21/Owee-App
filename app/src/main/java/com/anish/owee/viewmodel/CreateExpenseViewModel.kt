package com.anish.owee.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.owee.data.repository.*
import com.anish.owee.viewmodel.state.CreateExpenseUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale
import com.anish.owee.data.model.OweeNotification

class CreateExpenseViewModel : ViewModel() {

    private val groupRepository: GroupRepository =
        GroupRepositoryImpl()

    private val expenseRepository: ExpenseRepository =
        ExpenseRepositoryImpl()

    private val authRepository: AuthRepository = 
        AuthRepositoryImpl()

    private val notificationRepository: NotificationRepository = 
        NotificationRepositoryImpl()

    private val _uiState =
        MutableStateFlow(CreateExpenseUiState())

    val uiState: StateFlow<CreateExpenseUiState> =
        _uiState.asStateFlow()

    fun loadMembers(groupId: String) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true
                )

            try {

                val members =
                    groupRepository.getGroupMembers(groupId)

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        members = members
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

    fun updateTitle(title: String) {

        _uiState.value =
            _uiState.value.copy(
                title = title
            )
    }

    fun updateAmount(amount: String) {

        _uiState.value =
            _uiState.value.copy(
                amount = amount
            )
    }

    fun toggleParticipant(userId: String) {

        val selected =
            _uiState.value.selectedParticipantIds.toMutableSet()

        val customAmounts = _uiState.value.customAmounts.toMutableMap()

        if (selected.contains(userId)) {
            selected.remove(userId)
            customAmounts.remove(userId)
        } else {
            selected.add(userId)
        }

        _uiState.value =
            _uiState.value.copy(
                selectedParticipantIds = selected,
                customAmounts = customAmounts
            )
    }

    fun updateSplitMode(isCustom: Boolean) {
        val selected = _uiState.value.selectedParticipantIds
        
        // When switching to custom split, ensure everyone is "selected" to show textboxes
        val newSelected = if (isCustom) {
            _uiState.value.members.map { it.id }.toSet()
        } else {
            selected
        }

        _uiState.value = _uiState.value.copy(
            isCustomSplit = isCustom,
            selectedParticipantIds = newSelected
        )
    }

    fun updateCustomAmount(userId: String, amount: String) {
        val customAmounts = _uiState.value.customAmounts.toMutableMap()
        customAmounts[userId] = amount
        _uiState.value = _uiState.value.copy(customAmounts = customAmounts)
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }

    fun createExpense(
        groupId: String
    ) {

        val totalAmount =
            _uiState.value.amount.toDoubleOrNull()
                ?: return

        val currentUser =
            groupRepository.getCurrentUserId()
                ?: return

        val selectedIds = _uiState.value.selectedParticipantIds
        if (selectedIds.isEmpty()) return

        val participantShares = mutableMapOf<String, Double>()

        if (_uiState.value.isCustomSplit) {
            // Custom Split
            var sum = 0.0
            selectedIds.forEach { id ->
                val share = _uiState.value.customAmounts[id]?.toDoubleOrNull() ?: 0.0
                participantShares[id] = share
                sum += share
            }

            if (kotlin.math.abs(sum - totalAmount) > 0.01) {
                _uiState.value = _uiState.value.copy(
                    error = "Total split (₹$sum) doesn't match expense amount (₹$totalAmount)"
                )
                return
            }
        } else {
            // Equal Split
            val share = totalAmount / selectedIds.size
            selectedIds.forEach { id ->
                participantShares[id] = share
            }
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            val result =
                expenseRepository.createExpense(
                    groupId = groupId,
                    title = _uiState.value.title,
                    amount = totalAmount,
                    payerId = currentUser,
                    participants = participantShares
                )

            result.onSuccess {
                val title = _uiState.value.title
                
                // Send notifications to all participants
                viewModelScope.launch {
                    try {
                        val currentUser = authRepository.getCurrentUser()
                        val group = groupRepository.getGroup(groupId)
                        
                        participantShares.forEach { (userId, shareAmount) ->
                            if (userId != currentUser?.id) {
                                val notification = OweeNotification(
                                    senderId = currentUser?.id ?: "",
                                    receiverId = userId,
                                    type = "group_expense",
                                    title = group?.name ?: "New Expense",
                                    body = "${currentUser?.displayName} added \"$title\": ₹${String.format(Locale.US, "%.2f", shareAmount)}",
                                    data = buildJsonObject {
                                        put("type", "group_expense")
                                        put("group_name", group?.name ?: "")
                                        put("payer_name", currentUser?.displayName ?: "Someone")
                                        put("amount", shareAmount)
                                        put("expense_title", title)
                                        currentUser?.photoUrl?.let { put("sender_photo", it) }
                                    }
                                )
                                notificationRepository.sendNotification(notification)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OWEE_NOTIFICATION", "Failed to send expense notifications", e)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    title = "",
                    amount = "",
                    customAmounts = emptyMap(),
                    error = null
                )
            }

            result.onFailure {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error = it.message
                    )
            }
        }
    }
}