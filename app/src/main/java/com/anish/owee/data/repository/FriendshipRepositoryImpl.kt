package com.anish.owee.data.repository

import com.anish.owee.data.model.Friendship
import com.anish.owee.data.model.SearchUser
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID

class FriendshipRepositoryImpl : FriendshipRepository {

    private val client = SupabaseProvider.client
    private val auth = client.auth
    private val postgrest = client.postgrest

    private val selectColumns = Columns.raw("*, sender:sender_id(*), receiver:receiver_id(*)")

    override suspend fun sendFriendRequest(
        receiverId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        val currentUserId =
            auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(
                    Exception("User not authenticated")
                )

        try {
            postgrest["friendships"].insert(
                mapOf(
                    "sender_id" to currentUserId,
                    "receiver_id" to receiverId,
                    "status" to "pending"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(
        friendshipId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {
            postgrest["friendships"].update(
                {
                    set("status", "accepted")
                }
            ) {
                filter {
                    eq("id", friendshipId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectFriendRequest(
        friendshipId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {
            postgrest["friendships"].update(
                {
                    set("status", "rejected")
                }
            ) {
                filter {
                    eq("id", friendshipId)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIncomingRequests(): List<Friendship> =
        withContext(Dispatchers.IO) {

            val currentUserId =
                auth.currentUserOrNull()?.id
                    ?: return@withContext emptyList()

            try {
                postgrest["friendships"]
                    .select(selectColumns) {
                        filter {
                            eq("receiver_id", currentUserId)
                            eq("status", "pending")
                        }
                    }
                    .decodeList<Friendship>()
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun getOutgoingRequests(): List<Friendship> =
        withContext(Dispatchers.IO) {

            val currentUserId =
                auth.currentUserOrNull()?.id
                    ?: return@withContext emptyList()

            try {
                postgrest["friendships"]
                    .select(selectColumns) {
                        filter {
                            eq("sender_id", currentUserId)
                            eq("status", "pending")
                        }
                    }
                    .decodeList<Friendship>()
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun getAcceptedFriendships(): List<Friendship> =
        withContext(Dispatchers.IO) {

            val currentUserId =
                auth.currentUserOrNull()?.id
                    ?: return@withContext emptyList()

            try {
                postgrest["friendships"]
                    .select(selectColumns) {
                        filter {
                            eq("status", "accepted")

                            or {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                    }
                    .decodeList<Friendship>()
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun searchUsers(
        query: String
    ): List<SearchUser> = withContext(Dispatchers.IO) {

        if (query.isBlank()) {
            return@withContext emptyList()
        }

        try {

            postgrest["users"]
                .select {
                    filter {
                        ilike(
                            "username",
                            "%$query%"
                        )
                    }
                }
                .decodeList<SearchUser>()

        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    override fun friendshipChanges(): Flow<Unit> = flow {
        val channelId = "friendship_changes_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)

        try {
            val postgresFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "friendships"
            }

            client.realtime.connect()
            client.realtime.status.first { it == Realtime.Status.CONNECTED }

            channel.subscribe()
            channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }

            postgresFlow.collect {
                emit(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("FriendshipRepo", "Error in friendshipChanges", e)
            throw e
        } finally {
            channel.unsubscribe()
            client.realtime.removeChannel(channel)
        }
    }
}