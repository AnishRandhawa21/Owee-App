package com.anish.owee.data.repository

import com.anish.owee.data.model.Expense
import com.anish.owee.data.model.ExpenseParticipant
import com.anish.owee.data.model.ExpenseParticipantUser

interface ExpenseRepository {

    suspend fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        payerId: String,
        participantIds: List<String>
    ): Result<Unit>

    suspend fun getGroupExpenses(
        groupId: String
    ): List<Expense>

    suspend fun getExpenseParticipants(
        expenseId: String
    ): List<ExpenseParticipantUser>

    suspend fun getRawExpenseParticipants(
        expenseId: String
    ): List<ExpenseParticipant>
}