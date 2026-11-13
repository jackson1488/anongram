package com.anongram.web.features.auth.domain.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val identity: UserIdentity) : AuthState
}
