package com.anish.owee.data.repository

import com.anish.owee.data.model.Expense
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.anish.owee.data.model.CreateExpenseRequest
import com.anish.owee.data.model.CreateExpenseParticipantRequest
import com.anish.owee.data.model.ExpenseParticipant
import com.anish.owee.data.model.ExpenseParticipantUser

class ExpenseRepositoryImpl : ExpenseRepository {

    private val client = SupabaseProvider.client

    private val postgrest = client.postgrest

    override suspend fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        payerId: String,
        participantIds: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {

            val expense = postgrest["expenses"]
                .insert(
                    CreateExpenseRequest(
                        groupId = groupId,
                        payerId = payerId,
                        title = title,
                        amount = amount
                    )
                ) {
                    select()
                }
                .decodeSingle<Expense>()

            val shareAmount =
                amount / participantIds.size

            participantIds.forEach { userId ->

                postgrest["expense_participants"]
                    .insert(
                        CreateExpenseParticipantRequest(
                            expenseId = expense.id,
                            userId = userId,
                            shareAmount = shareAmount
                        )
                    )
            }

            Result.success(Unit)

        } catch (e: Exception) {

            android.util.Log.e(
                "OWEE_EXPENSE",
                "Create expense failed",
                e
            )

            Result.failure(e)
        }
    }

    override suspend fun getGroupExpenses(
        groupId: String
    ): List<Expense> = withContext(Dispatchers.IO) {

        try {

            postgrest["expenses"]
                .select {
                    filter {
                        eq("group_id", groupId)
                    }
                }
                .decodeList<Expense>()

        } catch (e: Exception) {

            android.util.Log.e(
                "OWEE_EXPENSE",
                "Load expenses failed",
                e
            )

            emptyList()
        }
    }

    override suspend fun getExpenseParticipants(
        expenseId: String
    ): List<ExpenseParticipantUser> =
        withContext(Dispatchers.IO) {

            try {

                postgrest["expense_participants"]
                    .select(
                        columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                            "share_amount,user:users(*)"
                        )
                    ) {
                        filter {
                            eq("expense_id", expenseId)
                        }
                    }
                    .decodeList<ExpenseParticipantUser>()

            } catch (e: Exception) {

                android.util.Log.e(
                    "OWEE_EXPENSE",
                    "Load participants failed",
                    e
                )

                emptyList()
            }
        }

    override suspend fun getRawExpenseParticipants(
        expenseId: String
    ): List<ExpenseParticipant> =
        withContext(Dispatchers.IO) {

            try {

                postgrest["expense_participants"]
                    .select {
                        filter {
                            eq("expense_id", expenseId)
                        }
                    }
                    .decodeList<ExpenseParticipant>()

            } catch (e: Exception) {

                android.util.Log.e(
                    "OWEE_EXPENSE",
                    "Load raw participants failed",
                    e
                )

                emptyList()
            }
        }

    override suspend fun getGroupExpenseParticipants(
        groupId: String
    ): List<ExpenseParticipant> =
        withContext(Dispatchers.IO) {

            try {

                postgrest["expense_participants"]
                    .select(
                        columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                            """
                        *,
                        expense:expenses!inner(
                            group_id
                        )
                        """.trimIndent()
                        )
                    ) {
                        filter {
                            eq(
                                "expense.group_id",
                                groupId
                            )
                        }
                    }
                    .decodeList<ExpenseParticipant>()

            } catch (e: Exception) {

                android.util.Log.e(
                    "OWEE_EXPENSE",
                    "Load group participants failed",
                    e
                )

                emptyList()
            }
        }

    override suspend fun getAllExpenseParticipants(
        expenseIds: List<String>
    ): List<ExpenseParticipant> =
        withContext(Dispatchers.IO) {

            try {

                postgrest["expense_participants"]
                    .select {
                        filter {
                            isIn(
                                "expense_id",
                                expenseIds
                            )
                        }
                    }
                    .decodeList<ExpenseParticipant>()

            } catch (e: Exception) {

                android.util.Log.e(
                    "OWEE_EXPENSE",
                    "Load all participants failed",
                    e
                )

                emptyList()
            }
        }
}