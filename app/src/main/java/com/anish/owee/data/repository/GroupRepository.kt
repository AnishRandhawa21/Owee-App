package com.anish.owee.data.repository

import com.anish.owee.data.model.Group
import com.anish.owee.data.model.User
import kotlinx.coroutines.flow.Flow

interface GroupRepository {

    suspend fun createGroup(
        name: String,
        memberIds: List<String>
    ): Result<Unit>

    suspend fun getGroups(): List<Group>

    suspend fun getGroupMembers(
        groupId: String
    ): List<User>

    fun getCurrentUserId(): String?

    fun groupChanges(): Flow<Unit>
}