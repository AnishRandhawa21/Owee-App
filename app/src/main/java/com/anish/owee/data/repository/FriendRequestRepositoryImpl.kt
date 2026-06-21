package com.anish.owee.data.repository

import com.anish.owee.data.model.CreateFriendRequest
import com.anish.owee.data.model.FriendRequest
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
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

class FriendRequestRepositoryImpl : FriendRequestRepository {

    private val client = SupabaseProvider.client

    private val auth = client.auth

    private val postgrest = client.postgrest

    override suspend fun createRequest(
        friendId: String,
        amount: Double,
        note: String?
    ): Result<Unit> {
        val currentUserId =
            auth.currentUserOrNull()?.id
                ?: return Result.failure(
                    Exception("User not authenticated")
                )

        return try {

            postgrest["friend_requests"].insert(
                CreateFriendRequest(
                    creatorId = currentUserId,
                    friendId = friendId,
                    amount = amount,
                    note = note
                )
            )

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    override suspend fun getRequestsForFriend(
        friendId: String
    ): List<FriendRequest> {

        val currentUserId =
            auth.currentUserOrNull()?.id
                ?: return emptyList()

        return try {

            android.util.Log.d(
                "OWEE_REQUESTS",
                "Loading requests between $currentUserId and $friendId"
            )

            val requests = postgrest["friend_requests"]
                .select()
                .decodeList<FriendRequest>()
                .filter {

                    (it.creatorId == currentUserId &&
                            it.friendId == friendId)

                            ||

                            (it.creatorId == friendId &&
                                    it.friendId == currentUserId)
                }

            android.util.Log.d(
                "OWEE_REQUESTS",
                "Filtered requests = ${requests.size}"
            )

            requests

        } catch (e: Exception) {

            android.util.Log.e(
                "OWEE_REQUESTS",
                "Error loading filtered requests",
                e
            )

            emptyList()
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    override suspend fun markRequestPaid(
        requestId: String
    ): Result<Unit> {

        return try {

            postgrest["friend_requests"]
                .update(
                    {
                        set("status", "paid")
                    }
                ) {
                    filter {
                        eq("id", requestId)
                    }
                }

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    override fun requestChanges(): Flow<Unit> = flow {
        val channelId = "request_changes_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)

        try {
            android.util.Log.d("OWEE_REALTIME", "Attempting to subscribe to friend_requests")

            val postgresFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "friend_requests"
            }

            channel.subscribe()
            channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }
            
            android.util.Log.d("OWEE_REALTIME", "Subscribed successfully to friend_requests")

            postgresFlow.collect {
                android.util.Log.d("OWEE_REALTIME", "Change detected in friend_requests")
                emit(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("OWEE_REALTIME", "Error in requestChanges flow", e)
        } finally {
            channel.unsubscribe()
            client.realtime.removeChannel(channel)
        }
    }
}