package com.todo.server.auth

import com.todo.server.database.UserRecord
import com.todo.server.database.UserRepository
import com.todo.server.http.invalidCredentials

/** Self-hosted auth backed by bcrypt password hashes and HMAC JWTs. */
class LocalAuthProvider(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val jwt: JwtService,
) : AuthProvider {
    override suspend fun register(name: String, email: String, password: String): AuthResult {
        val user = users.create(
            name = name.trim(),
            email = email.trim(),
            passwordHash = hasher.hash(password),
        )
        return AuthResult(user, jwt.issueToken(user.id))
    }

    override suspend fun login(email: String, password: String): AuthResult {
        val record = users.findByEmail(email) ?: throw invalidCredentials()
        if (!hasher.verify(password, record.passwordHash)) throw invalidCredentials()
        return AuthResult(record, jwt.issueToken(record.id))
    }

    override suspend fun logout(token: String) = Unit

    override fun verifyToken(token: String): String? = jwt.verifyToken(token)
}
