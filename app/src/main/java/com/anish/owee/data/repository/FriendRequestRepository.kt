package com.anish.owee.data.repository

import com.anish.owee.data.model.FriendRequest

interface FriendRequestRepository {

    suspend fun createRequest(
        friendId: String,
        amount: Double,
        note: String?
    ): Result<Unit>

    suspend fun getRequestsForFriend(
        friendId: String
    ): List<FriendRequest>

    fun getCurrentUserId(): String?

    suspend fun markRequestPaid(
        requestId: String
    ): Result<Unit>

    suspend fun deleteRequest(
        requestId: String
    ): Result<Unit>

    fun requestChanges(): kotlinx.coroutines.flow.Flow<Unit>
}
