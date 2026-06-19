package com.anish.owee.data.repository

import com.anish.owee.data.model.SearchUser

interface UserSearchRepository {

    suspend fun searchUsers(
        query: String
    ): List<SearchUser>
}