package com.anish.owee.data.repository

import com.anish.owee.data.model.CreateFriendRequest
import com.anish.owee.data.model.FriendRequest
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

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
}