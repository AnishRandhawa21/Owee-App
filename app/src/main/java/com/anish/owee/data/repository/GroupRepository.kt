package com.anish.owee.data.repository

import com.anish.owee.data.model.Group
import com.anish.owee.data.model.User
import com.anish.owee.viewmodel.state.GroupWithMetadata
import kotlinx.coroutines.flow.Flow

interface GroupRepository {

    suspend fun createGroup(
        name: String,
        memberIds: List<String>
    ): Result<Unit>

    suspend fun getGroups(): List<Group>

    suspend fun getGroupsWithMetadata(): List<GroupWithMetadata>

    suspend fun getGroupMembers(
        groupId: String
    ): List<User>

    suspend fun getGroup(
        groupId: String
    ): Group?

    suspend fun deleteGroup(
        groupId: String
    ): Result<Unit>

    fun getCurrentUserId(): String?

    fun groupChanges(): Flow<Unit>
}