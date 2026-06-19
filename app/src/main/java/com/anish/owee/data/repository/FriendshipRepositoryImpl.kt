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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        android.util.Log.d("OWEE_REALTIME", "friendshipChanges() flow start")
        
        val channel = client.realtime.channel("friendship_changes")

        coroutineScope {
            // Observe Channel Status
            launch {
                channel.status.collect { status ->
                    android.util.Log.d("OWEE_REALTIME", "CHANNEL_STATUS: $status")
                }
            }

            // Observe Realtime Socket Status
            launch {
                client.realtime.status.collect { status ->
                    android.util.Log.d("OWEE_REALTIME", "REALTIME_STATUS: $status")
                }
            }

            try {
                // Initialize postgres flow listener
                val postgresFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "friendships"
                }

                android.util.Log.d("OWEE_REALTIME", "Connecting to Realtime socket...")
                client.realtime.connect()

                // Wait for the socket to reach CONNECTED status before subscribing
                android.util.Log.d("OWEE_REALTIME", "Waiting for REALTIME_STATUS to be CONNECTED...")
                client.realtime.status.first { it == Realtime.Status.CONNECTED }
                android.util.Log.d("OWEE_REALTIME", "REALTIME_STATUS is CONNECTED")

                android.util.Log.d("OWEE_REALTIME", "Subscribing to channel: friendship_changes")
                channel.subscribe()
                
                // Wait for the channel to reach SUBSCRIBED status
                android.util.Log.d("OWEE_REALTIME", "Waiting for CHANNEL_STATUS to be SUBSCRIBED...")
                channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }
                android.util.Log.d("OWEE_REALTIME", "CHANNEL_STATUS is SUBSCRIBED")
                
                android.util.Log.d("OWEE_REALTIME", "Waiting for postgres events...")
                postgresFlow.collect { action ->
                    android.util.Log.d("OWEE_REALTIME", "Database event received: $action")
                    emit(Unit)
                }
            } catch (e: Exception) {
                android.util.Log.e("OWEE_REALTIME", "Realtime flow error: ${e.message}", e)
                // In case of error, we can either re-throw or just end the flow.
                // Re-throwing allows the ViewModel to catch it.
                throw e
            }
        }
    }
}