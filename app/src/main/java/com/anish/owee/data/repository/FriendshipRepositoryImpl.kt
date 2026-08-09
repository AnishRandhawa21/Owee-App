package com.anish.owee.data.repository

import android.util.Log
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
    private val preferenceManager = com.anish.owee.data.local.PreferenceManager.getInstance(com.anish.owee.OweeApp.instance)

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
            // 1. Check if a friendship row already exists
            val existingFriendship = postgrest["friendships"]
                .select {
                    filter {
                        or {
                            and {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", receiverId)
                            }
                            and {
                                eq("sender_id", receiverId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                    }
                }
                .decodeSingleOrNull<Friendship>()
            android.util.Log.d(
                "OWEE_FRIEND",
                "Existing friendship = $existingFriendship"
            )

            if (existingFriendship != null) {
                when (existingFriendship.status) {
                    "pending" -> return@withContext Result.failure(Exception("Friend request already sent"))
                    "accepted" -> return@withContext Result.failure(Exception("Already friends"))
                    "rejected" -> {
                        Log.d("OWEE_FRIEND", "Entering rejected branch")
                        // 2. Update status back to 'pending' if it was rejected
                        Log.d("OWEE_FRIEND", "Updating friendship ${existingFriendship.id}")
                        postgrest["friendships"].update(
                            {
                                set("status", "pending")
                                set("sender_id", currentUserId) // In case the original direction was different
                                set("receiver_id", receiverId)
                                Log.d("OWEE_FRIEND", "Update completed")
                            }
                        )
                        {
                            filter {
                                eq("id", existingFriendship.id)
                            }
                        }
                        return@withContext Result.success(Unit)
                    }
                }
            }

            // 3. No row exists, insert new one
            postgrest["friendships"].insert(
                mapOf(
                    "sender_id" to currentUserId,
                    "receiver_id" to receiverId,
                    "status" to "pending"
                )
            )

            Result.success(Unit)
        }catch (e: Exception) {
            android.util.Log.e(
                "OWEE_FRIEND",
                "sendFriendRequest failed",
                e
            )

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

    override suspend fun removeFriendship(friendshipId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch friendship details to get the IDs of the two users
                val friendship = postgrest["friendships"]
                    .select { filter { eq("id", friendshipId) } }
                    .decodeSingleOrNull<Friendship>() 
                    ?: return@withContext Result.failure(Exception("Friendship not found"))

                val user1 = friendship.senderId
                val user2 = friendship.receiverId

                // 2. Delete the friendship record
                postgrest["friendships"].delete {
                    filter { eq("id", friendshipId) }
                }

                // 3. Delete all money requests between these two users (friend_requests table)
                postgrest["friend_requests"].delete {
                    filter {
                        or {
                            and {
                                eq("creator_id", user1)
                                eq("friend_id", user2)
                            }
                            and {
                                eq("creator_id", user2)
                                eq("friend_id", user1)
                            }
                        }
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("OWEE_FRIEND", "Cleanup failed", e)
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
                getCurrentUserId()
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
                throw e
            }
        }

    override suspend fun getFriendships(): List<Friendship> =
        withContext(Dispatchers.IO) {

            val currentUserId =
                getCurrentUserId()
                    ?: return@withContext emptyList()

            try {
                postgrest["friendships"]
                    .select(selectColumns) {
                        filter {
                            neq("status", "rejected")

                            or {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                    }
                    .decodeList<Friendship>()
            } catch (e: Exception) {
                throw e
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
                        eq(
                            "username",
                            query
                        )
                    }
                }
                .decodeList<SearchUser>()

        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id ?: preferenceManager.getUser()?.id
    }

    override fun friendshipChanges(): Flow<Unit> = flow {
        val channelId = "friendship_changes_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)

        try {
            android.util.Log.d("OWEE_REALTIME", "Attempting to subscribe to friendships")

            val postgresFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "friendships"
            }

            channel.subscribe()
            channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }

            android.util.Log.d("OWEE_REALTIME", "Subscribed successfully to friendships")

            postgresFlow.collect {
                android.util.Log.d("OWEE_REALTIME", "Change detected in friendships")
                emit(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("OWEE_REALTIME", "Error in friendshipChanges flow", e)
        } finally {
            channel.unsubscribe()
            client.realtime.removeChannel(channel)
        }
    }
}