package com.todo.server.database

import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

object Users : Table("users") {
    val id: Column<Uuid> = uuid("id")
    val name = varchar("name", 100)
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object TodoLists : Table("todo_lists") {
    val id: Column<Uuid> = uuid("id")
    val name = varchar("name", 200)
    val ownerId = reference("owner_id", Users.id)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object ListMembers : Table("list_members") {
    val listId = reference("list_id", TodoLists.id)
    val userId = reference("user_id", Users.id)
    val role = varchar("role", 20)
    val joinedAt = long("joined_at")

    override val primaryKey = PrimaryKey(listId, userId)
}

object Todos : Table("todos") {
    val id: Column<Uuid> = uuid("id")
    val listId = reference("list_id", TodoLists.id)
    val title = varchar("title", 300)
    val description = text("description")
    val completed = bool("completed")
    val createdBy = reference("created_by", Users.id).nullable()
    val assignedTo = reference("assigned_to", Users.id).nullable()
    val position = long("position")
    val dueDate = long("due_date").nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
