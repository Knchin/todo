package com.todo.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Strongly-typed realtime events pushed by the server to every connected
 * client subscribed to the affected list.
 *
 * The Kotlin serialization `type` discriminator (from the class name) is
 * used on the wire; `@SerialName` keeps the wire names stable even if
 * class names change.
 */
@Serializable
sealed interface RealtimeEvent {
    val listId: String
}

@Serializable
@SerialName("todo.created")
data class TodoCreated(val todo: TodoDto) : RealtimeEvent {
    override val listId: String get() = todo.listId
}

@Serializable
@SerialName("todo.updated")
data class TodoUpdated(val todo: TodoDto) : RealtimeEvent {
    override val listId: String get() = todo.listId
}

@Serializable
@SerialName("todo.completed")
data class TodoCompleted(val todo: TodoDto) : RealtimeEvent {
    override val listId: String get() = todo.listId
}

@Serializable
@SerialName("todo.deleted")
data class TodoDeleted(
    val todoId: String,
    override val listId: String,
) : RealtimeEvent

@Serializable
@SerialName("todo.reordered")
data class TodoReordered(
    val orderedIds: List<String>,
    override val listId: String,
) : RealtimeEvent

@Serializable
@SerialName("list.updated")
data class ListUpdated(val list: TodoListDto) : RealtimeEvent {
    override val listId: String get() = list.id
}

@Serializable
@SerialName("list.deleted")
data class ListDeleted(override val listId: String) : RealtimeEvent

@Serializable
@SerialName("member.added")
data class MemberAdded(
    val member: MemberDto,
    override val listId: String,
) : RealtimeEvent

@Serializable
@SerialName("member.removed")
data class MemberRemoved(
    val userId: String,
    override val listId: String,
) : RealtimeEvent

@Serializable
@SerialName("member.role_changed")
data class MemberRoleChanged(
    val userId: String,
    val role: ListRole,
    override val listId: String,
) : RealtimeEvent

@Serializable
@SerialName("server.error")
data class ServerErrorEvent(
    val code: String,
    val message: String,
    override val listId: String = "",
) : RealtimeEvent

/** Messages sent by a client to the realtime server. */
@Serializable
sealed interface ClientMessage

@Serializable
@SerialName("subscribe")
data class Subscribe(val listId: String) : ClientMessage

@Serializable
@SerialName("unsubscribe")
data class Unsubscribe(val listId: String) : ClientMessage
