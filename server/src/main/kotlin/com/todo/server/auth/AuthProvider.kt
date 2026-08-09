package com.todo.server.auth

import com.todo.server.database.UserRecord

/**
 * Backend for authentication. Two implementations exist:
 *  - [LocalAuthProvider] — self-hosted bcrypt + HMAC-JWT (tests / local dev).
 *  - [SupabaseAuthProvider] — delegates credentials to Supabase Auth (GoTrue)
 *    and validates GoTrue JWTs via JWKS.
 * The active provider is chosen from configuration in `Application.module`.
 */
interface AuthProvider {
    suspend fun register(name: String, email: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun logout(token: String)
    /** Returns the user id if the token is valid and unexpired, otherwise null. */
    fun verifyToken(token: String): String?
}

data class AuthResult(val user: UserRecord, val token: String)
