package com.anish.owee.data.repository

import com.anish.owee.data.model.Friendship
import com.anish.owee.data.model.SearchUser
import kotlinx.coroutines.flow.Flow

interface FriendshipRepository {

    suspend fun sendFriendRequest(
        receiverId: String
    ): Result<Unit>

    suspend fun acceptFriendRequest(
        friendshipId: String
    ): Result<Unit>

    suspend fun rejectFriendRequest(
        friendshipId: String
    ): Result<Unit>

    suspend fun getIncomingRequests(): List<Friendship>

    suspend fun getOutgoingRequests(): List<Friendship>

    suspend fun getAcceptedFriendships(): List<Friendship>

    suspend fun getFriendships(): List<Friendship>

    suspend fun removeFriendship(
        friendshipId: String
    ): Result<Unit>

    suspend fun searchUsers(
        query: String
    ): List<SearchUser>

    fun getCurrentUserId(): String?

    fun friendshipChanges(): Flow<Unit>
}