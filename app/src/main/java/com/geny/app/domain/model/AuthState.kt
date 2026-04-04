package com.geny.app.domain.model

sealed class AuthState {
    data object NoServerUrl : AuthState()
    data object NoUsers : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(
        val username: String,
        val displayName: String? = null
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}
