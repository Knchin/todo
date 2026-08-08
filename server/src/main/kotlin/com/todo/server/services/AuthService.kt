package com.todo.server.services

import com.todo.server.auth.PasswordHasher
import com.todo.server.auth.JwtService
import com.todo.server.database.UserRecord
import com.todo.server.database.UserRepository
import com.todo.server.http.conflict
import com.todo.server.http.invalidCredentials
import com.todo.server.http.validationError
import com.todo.shared.model.LoginRequest
import com.todo.shared.model.RegisterRequest
import com.todo.shared.validation.Validation

class AuthService(
    private val users: UserRepository,
    private val hasher: PasswordHasher,
    private val jwt: JwtService,
) {
    suspend fun register(request: RegisterRequest): UserRecord {
        validateRegister(request)
        if (users.findByEmail(request.email) != null) {
            throw conflict("An account with this email already exists.")
        }
        return users.create(
            name = request.name.trim(),
            email = request.email.trim(),
            passwordHash = hasher.hash(request.password),
        )
    }

    suspend fun login(request: LoginRequest): UserRecord {
        val record = users.findByEmail(request.email) ?: throw invalidCredentials()
        if (!hasher.verify(request.password, record.passwordHash)) {
            throw invalidCredentials()
        }
        return record
    }

    private fun validateRegister(request: RegisterRequest) {
        val nameError = Validation.name(request.name)
        if (!nameError.valid) throw validationError(nameError.error!!)
        val emailError = Validation.email(request.email)
        if (!emailError.valid) throw validationError(emailError.error!!)
        val passwordError = Validation.password(request.password)
        if (!passwordError.valid) throw validationError(passwordError.error!!)
    }

    fun tokenFor(user: UserRecord): String = jwt.issueToken(user.id)
}
