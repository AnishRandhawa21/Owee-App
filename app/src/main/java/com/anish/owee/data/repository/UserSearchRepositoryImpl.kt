package com.anish.owee.data.repository

import com.anish.owee.data.model.SearchUser
import com.anish.owee.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserSearchRepositoryImpl : UserSearchRepository {

    private val postgrest =
        SupabaseProvider.client.postgrest

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
}