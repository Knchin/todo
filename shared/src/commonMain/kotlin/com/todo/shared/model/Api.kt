package com.todo.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val user: UserDto,
)

@Serializable
data class CreateListRequest(
    val name: String,
)

@Serializable
data class UpdateListRequest(
    val name: String,
)

@Serializable
data class CreateTodoRequest(
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,
    val assignedTo: String? = null,
)

/**
 * Partial update for a todo. `null` fields are left unchanged.
 * `completed` uses a Boolean? because false is a meaningful value.
 * Use the explicit `clear*` flags to set a nullable field back to null.
 */
@Serializable
data class UpdateTodoRequest(
    val title: String? = null,
    val description: String? = null,
    val completed: Boolean? = null,
    val assignedTo: String? = null,
    val clearAssignedTo: Boolean = false,
    val dueDate: Long? = null,
    val clearDueDate: Boolean = false,
)

@Serializable
data class AddMemberRequest(
    val email: String,
    val role: ListRole? = null,
)

@Serializable
data class UpdateMemberRoleRequest(
    val role: ListRole,
)

@Serializable
data class ReorderTodosRequest(
    val orderedIds: List<String>,
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody,
)

/** Stable machine-readable error codes shared by server and clients. */
object ErrorCodes {
    const val VALIDATION = "VALIDATION_ERROR"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val FORBIDDEN = "FORBIDDEN"
    const val NOT_FOUND = "NOT_FOUND"
    const val CONFLICT = "CONFLICT"
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val INTERNAL = "INTERNAL_ERROR"
}
