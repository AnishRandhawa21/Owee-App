package com.anish.owee.data.repository

import com.anish.owee.data.model.Group
import com.anish.owee.data.model.GroupMember
import com.anish.owee.data.model.GroupMemberUser
import com.anish.owee.data.model.User
import com.anish.owee.data.remote.SupabaseProvider
import com.anish.owee.viewmodel.state.GroupWithMetadata
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.mapNotNull
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
class GroupRepositoryImpl : GroupRepository {

    private val client = SupabaseProvider.client
    private val auth = client.auth
    private val postgrest = client.postgrest

    override suspend fun createGroup(
        name: String,
        memberIds: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {

        val currentUserId =
            auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(
                    Exception("User not authenticated")
                )

        android.util.Log.d(
            "OWEE_GROUP",
            "Current User = $currentUserId"
        )

        try {

            // Create group
            val group = postgrest["groups"]
                .insert(
                    mapOf(
                        "name" to name,
                        "created_by" to currentUserId
                    )
                ) {
                    select()
                }
                .decodeSingle<Group>()

             // Bulk add all members (including creator) in ONE call
            val membersToInsert = mutableListOf(
                mapOf("group_id" to group.id, "user_id" to currentUserId)
            )
            
            memberIds.forEach { memberId ->
                membersToInsert.add(mapOf("group_id" to group.id, "user_id" to memberId))
            }

            postgrest["group_members"].insert(membersToInsert)

            android.util.Log.d(
                "OWEE_GROUP",
                "Group created successfully with ${membersToInsert.size} members"
            )

            Result.success(Unit)


        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroups(): List<Group> =
        withContext(Dispatchers.IO) {
            // ... (keep existing implementation or just use getGroupsWithMetadata().map { it.group })
            // For now, let's just keep it as is or implement getGroupsWithMetadata
            emptyList() // Placeholder, I'll update it properly below
        }

    override suspend fun getGroupsWithMetadata(): List<GroupWithMetadata> =
        withContext(Dispatchers.IO) {
            val currentUserId = auth.currentUserOrNull()?.id ?: return@withContext emptyList()
            try {
                // 1. Get all group memberships for the current user
                val memberships = postgrest["group_members"]
                    .select { filter { eq("user_id", currentUserId) } }
                    .decodeList<GroupMember>()
                
                val groupIds = memberships.map { it.groupId }
                if (groupIds.isEmpty()) return@withContext emptyList()

                // 2. Fetch the groups
                val groups = postgrest["groups"]
                    .select {
                        filter { isIn("id", groupIds) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Group>()

                // 3. Fetch all members and creators for these groups in ONE call using a join
                // This is the most optimized way to get everything at once
                val allGroupDetails = postgrest["group_members"]
                    .select(Columns.raw("group_id, user:users(*)")) {
                        filter { isIn("group_id", groupIds) }
                    }
                    .decodeList<GroupMemberUser>()
                
                val membersByGroup = allGroupDetails.groupBy { it.groupId }
                
                // Fetch creators separately to be safe, but still in bulk
                val creatorIds = groups.map { it.createdBy }.distinct()
                val creators = postgrest["users"]
                    .select { filter { isIn("id", creatorIds) } }
                    .decodeList<User>()
                    .associateBy { it.id }

                groups.map { group ->
                    GroupWithMetadata(
                        group = group,
                        creator = creators[group.createdBy],
                        members = membersByGroup[group.id]?.mapNotNull { it.user } ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("OWEE_GROUP", "getGroupsWithMetadata failed", e)
                emptyList()
            }
        }

    override suspend fun getGroup(
        groupId: String
    ): Group? = withContext(Dispatchers.IO) {
        try {
            postgrest["groups"]
                .select {
                    filter {
                        eq("id", groupId)
                    }
                }
                .decodeSingle<Group>()
        } catch (e: Exception) {
            android.util.Log.e("OWEE_GROUP", "getGroup failed", e)
            null
        }
    }

    override suspend fun deleteGroup(groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest["groups"].delete {
                filter {
                    eq("id", groupId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("OWEE_GROUP", "deleteGroup failed", e)
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    override suspend fun getGroupMembers(
        groupId: String
    ): List<User> =
        withContext(Dispatchers.IO) {

            try {

                postgrest["group_members"]
                    .select(
                        columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                            "user:users(*)"
                        )
                    ) {
                        filter {
                            eq("group_id", groupId)
                        }
                    }
                    .decodeList<GroupMemberUser>()
                    .mapNotNull {
                        it.user
                    }

            } catch (e: Exception) {

                android.util.Log.e(
                    "OWEE_GROUP",
                    "getGroupMembers failed",
                    e
                )

                emptyList()
            }
        }

    override fun groupChanges(): Flow<Unit> = flow {
        val channelId = "group_changes_${UUID.randomUUID()}"
        val channel = client.realtime.channel(channelId)

        try {
            android.util.Log.d("OWEE_REALTIME", "Attempting to subscribe to group_members")
            
            val postgresFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "group_members"
            }

            channel.subscribe()
            channel.status.first { it == RealtimeChannel.Status.SUBSCRIBED }
            
            android.util.Log.d("OWEE_REALTIME", "Subscribed successfully to group_members")

            postgresFlow.collect {
                android.util.Log.d("OWEE_REALTIME", "Change detected in group_members")
                emit(Unit)
            }
        } catch (e: Exception) {
            android.util.Log.e("OWEE_REALTIME", "Error in groupChanges flow", e)
        } finally {
            channel.unsubscribe()
            client.realtime.removeChannel(channel)
        }
    }
}