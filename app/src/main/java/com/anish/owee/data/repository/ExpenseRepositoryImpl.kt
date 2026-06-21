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
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.UUID

import io.github.jan.supabase.postgrest.query.Order

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
                    order("created_at", Order.DESCENDING)
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

    override fun expenseChanges(): Flow<Unit> = flow {
        val channelId = "expense_changes_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)

        try {
            android.util.Log.d("OWEE_REALTIME", "Attempting to subscribe to expenses")

            val postgresFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "expenses"
            }

            channel.subscribe()
            channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }
            
            android.util.Log.d("OWEE_REALTIME", "Subscribed successfully to expenses")

            postgresFlow.collect {
                android.util.Log.d("OWEE_REALTIME", "Change detected in expenses")
                emit(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("OWEE_REALTIME", "Error in expenseChanges flow", e)
        } finally {
            channel.unsubscribe()
            client.realtime.removeChannel(channel)
        }
    }
}