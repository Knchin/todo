package com.todo.server.auth

import com.todo.server.database.UserRecord
import com.todo.server.database.UserRepository
import com.todo.server.http.invalidCredentials
import com.todo.server.supabase.SupabaseAuthClient
import com.todo.server.supabase.SupabaseJwtVerifier

/**
 * Auth backed by Supabase Auth (GoTrue). Registration and login go through the
 * GoTrue REST API; the local `users` row is mirrored (keyed by the Supabase
 * user UUID) so list membership keeps working. Session tokens are Supabase
 * access tokens, validated against the project's JWKS.
 */
class SupabaseAuthProvider(
    private val users: UserRepository,
    private val client: SupabaseAuthClient,
    private val verifier: SupabaseJwtVerifier,
) : AuthProvider {
    override suspend fun register(name: String, email: String, password: String): AuthResult {
        val supabaseUser = client.signUp(email, password, name)
        val user = users.ensureExists(supabaseUser.id, name, email)
        return AuthResult(user, token = "")
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val session = client.signIn(email, password)
        val supabaseUser = session.user
            ?: client.getUser(session.accessToken)
            ?: throw invalidCredentials()
        val name = supabaseUser.userMetadata.name?.takeIf { it.isNotBlank() }
            ?: supabaseUser.email?.substringBefore('@')
            ?: email
        val user = users.ensureExists(supabaseUser.id, name, supabaseUser.email ?: email)
        return AuthResult(user, token = session.accessToken)
    }

    override suspend fun logout(token: String) {
        if (token.isNotBlank()) client.signOut(token)
    }

    override fun verifyToken(token: String): String? = verifier.verifyToken(token)
}
