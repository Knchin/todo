package com.todo.server.services

import com.todo.server.auth.AuthProvider
import com.todo.server.auth.AuthResult
import com.todo.server.http.conflict
import com.todo.server.database.UserRepository
import com.todo.server.http.validationError
import com.todo.shared.model.LoginRequest
import com.todo.shared.model.RegisterRequest
import com.todo.shared.validation.Validation

class AuthService(
    private val auth: AuthProvider,
    private val users: UserRepository,
) {
    suspend fun register(request: RegisterRequest): AuthResult {
        validateRegister(request)
        if (users.findByEmail(request.email) != null) {
            throw conflict("An account with this email already exists.")
        }
        return auth.register(request.name.trim(), request.email.trim(), request.password)
    }

    suspend fun login(request: LoginRequest): AuthResult {
        return auth.login(request.email.trim(), request.password)
    }

    suspend fun logout(token: String) {
        auth.logout(token)
    }

    private fun validateRegister(request: RegisterRequest) {
        val nameError = Validation.name(request.name)
        if (!nameError.valid) throw validationError(nameError.error!!)
        val emailError = Validation.email(request.email)
        if (!emailError.valid) throw validationError(emailError.error!!)
        val passwordError = Validation.password(request.password)
        if (!passwordError.valid) throw validationError(passwordError.error!!)
    }
}
