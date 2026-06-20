package com.anish.owee.data.repository

import com.anish.owee.data.model.Group
import com.anish.owee.data.model.GroupMember
import com.anish.owee.data.model.GroupMemberUser
import com.anish.owee.data.model.User
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
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

            // Add creator as member
            postgrest["group_members"].insert(
                mapOf(
                    "group_id" to group.id,
                    "user_id" to currentUserId
                )
            )


            // Add selected friends
            memberIds.forEach { memberId ->
                postgrest["group_members"].insert(
                    mapOf(
                        "group_id" to group.id,
                        "user_id" to memberId
                    )
                )
            }

            android.util.Log.d(
                "OWEE_GROUP",
                "Group created with ${memberIds.size + 1} members"
            )

            Result.success(Unit)


        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroups(): List<Group> =
        withContext(Dispatchers.IO) {

            val currentUserId =
                auth.currentUserOrNull()?.id
                    ?: return@withContext emptyList()

            try {

                val memberships = postgrest["group_members"]
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeList<GroupMember>()

                val groupIds = memberships.map {
                    it.groupId
                }

                android.util.Log.d(
                    "OWEE_GROUP",
                    "Group IDs = $groupIds"
                )

                if (groupIds.isEmpty()) {
                    return@withContext emptyList()
                }

                val groups = postgrest["groups"]
                    .select {
                        filter {
                            isIn("id", groupIds)
                        }
                    }
                    .decodeList<Group>()

                android.util.Log.d(
                    "OWEE_GROUP",
                    "Groups Loaded = ${groups.size}"
                )

                groups.forEach {
                    android.util.Log.d(
                        "OWEE_GROUP",
                        "Group = ${it.name}"
                    )
                }

                groups

            } catch (e: Exception) {

                android.util.Log.e(
                    "OWEE_GROUP",
                    "getGroups failed",
                    e
                )

                emptyList()
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

        val channelId =
            "group_changes_${UUID.randomUUID()}"

        val channel =
            client.realtime.channel(channelId)

        try {

            val postgresFlow =
                channel.postgresChangeFlow<PostgresAction>(
                    schema = "public"
                ) {
                    table = "group_members"
                }

            client.realtime.connect()

            client.realtime.status.first {
                it == Realtime.Status.CONNECTED
            }

            channel.subscribe()
            android.util.Log.d(
                "OWEE_GROUP",
                "Subscribed to group realtime"
            )

            channel.status.first {
                it == RealtimeChannel.Status.SUBSCRIBED
            }

            postgresFlow.collect {

                android.util.Log.d(
                    "OWEE_GROUP",
                    "Realtime event received"
                )
                emit(Unit)
            }

        } finally {

            channel.unsubscribe()

            client.realtime.removeChannel(channel)
        }
    }
}