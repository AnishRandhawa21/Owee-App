package com.anish.owee.data.repository

interface SessionRepository {

    suspend fun isLoggedIn(): Boolean

    suspend fun hasUsername(): Boolean

    suspend fun logout()
}