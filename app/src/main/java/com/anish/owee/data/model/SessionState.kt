package com.anish.owee.data.model

sealed interface SessionState {

    data object Loading : SessionState

    data object Unauthenticated : SessionState

    data object UsernameRequired : SessionState

    data object Authenticated : SessionState
}