package com.todo.shared.model

import kotlinx.serialization.Serializable

/**
 * Role of a user within a todo list.
 * Authorization is enforced server-side; this shared enum is also used by the
 * UI to enable/disable controls according to the current user's role.
 */
@Serializable
enum class ListRole {
    OWNER,
    EDITOR,
    VIEWER,
}

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val createdAt: Long,
)

@Serializable
data class MemberDto(
    val userId: String,
    val name: String,
    val email: String,
    val role: ListRole,
    val joinedAt: Long,
)

@Serializable
data class TodoListDto(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Role of the currently authenticated user in this list. */
    val role: ListRole,
    val members: List<MemberDto> = emptyList(),
)

@Serializable
data class TodoDto(
    val id: String,
    val listId: String,
    val title: String,
    val description: String,
    val completed: Boolean,
    val createdBy: String?,
    val createdByName: String? = null,
    val assignedTo: String?,
    val assignedToName: String? = null,
    val position: Long,
    val dueDate: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
