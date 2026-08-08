package com.todo.server.database

import com.todo.shared.model.TodoDto
import com.todo.shared.model.UpdateTodoRequest
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

private val createdByUser = Users.alias("creator")
private val assignedUser = Users.alias("assignee")

private val todoWithUsersQuery = Todos
    .leftJoin(createdByUser, { Todos.createdBy }, { createdByUser[Users.id] })
    .leftJoin(assignedUser, { Todos.assignedTo }, { assignedUser[Users.id] })

private fun ResultRow.toTodoDto() = TodoDto(
    id = this[Todos.id].toString(),
    listId = this[Todos.listId].toString(),
    title = this[Todos.title],
    description = this[Todos.description],
    completed = this[Todos.completed],
    createdBy = this[Todos.createdBy]?.toString(),
    createdByName = this.getOrNull(createdByUser[Users.name]),
    assignedTo = this[Todos.assignedTo]?.toString(),
    assignedToName = this.getOrNull(assignedUser[Users.name]),
    position = this[Todos.position],
    dueDate = this[Todos.dueDate],
    createdAt = this[Todos.createdAt],
    updatedAt = this[Todos.updatedAt],
)

class TodoRepository {
    suspend fun todosForList(listId: String): List<TodoDto> = withDb {
        todoWithUsersQuery
            .selectAll()
            .where { Todos.listId eq Uuid.parse(listId) }
            .orderBy(Todos.position to SortOrder.ASC, Todos.createdAt to SortOrder.ASC)
            .toList()
            .map { it.toTodoDto() }
    }

    suspend fun findById(todoId: String): TodoDto? = withDb {
        todoWithUsersQuery
            .selectAll()
            .where { Todos.id eq Uuid.parse(todoId) }
            .firstOrNull()
            ?.toTodoDto()
    }

    suspend fun create(
        listId: String,
        createdBy: String,
        title: String,
        description: String,
        dueDate: Long?,
        assignedTo: String?,
    ): TodoDto = withDb {
        val now = System.currentTimeMillis()
        val listUuid = Uuid.parse(listId)
        val id = Uuid.parse(java.util.UUID.randomUUID().toString())
        val nextPosition = (Todos
            .selectAll()
            .where { Todos.listId eq listUuid }
            .orderBy(Todos.position to SortOrder.DESC)
            .limit(1)
            .firstOrNull()?.get(Todos.position) ?: 0) + 1

        Todos.insert {
            it[Todos.id] = id
            it[Todos.listId] = listUuid
            it[Todos.title] = title
            it[Todos.description] = description
            it[Todos.completed] = false
            it[Todos.createdBy] = Uuid.parse(createdBy)
            it[Todos.assignedTo] = assignedTo?.let { Uuid.parse(it) }
            it[Todos.position] = nextPosition
            it[Todos.dueDate] = dueDate
            it[Todos.createdAt] = now
            it[Todos.updatedAt] = now
        }

        todoWithUsersQuery
            .selectAll()
            .where { Todos.id eq id }
            .firstOrNull()!!.toTodoDto()
    }

    /** Applies a partial update; returns the refreshed todo, or null when not found. */
    suspend fun update(todoId: String, request: UpdateTodoRequest): TodoDto? = withDb {
        val uid = Uuid.parse(todoId)
        val assignedTo = request.assignedTo
        val dueDate = request.dueDate
        val result = Todos.update({ Todos.id eq uid }) { statement ->
            request.title?.let { statement[Todos.title] = it.trim() }
            request.description?.let { statement[Todos.description] = it }
            request.completed?.let { statement[Todos.completed] = it }
            when {
                request.clearAssignedTo -> statement[Todos.assignedTo] = null
                assignedTo != null -> statement[Todos.assignedTo] = Uuid.parse(assignedTo)
            }
            when {
                request.clearDueDate -> statement[Todos.dueDate] = null
                dueDate != null -> statement[Todos.dueDate] = dueDate
            }
            statement[Todos.updatedAt] = System.currentTimeMillis()
        }
        if (result != 1) return@withDb null
        todoWithUsersQuery
            .selectAll()
            .where { Todos.id eq uid }
            .firstOrNull()
            ?.toTodoDto()
    }

    suspend fun delete(todoId: String): Boolean = withDb {
        Todos.deleteWhere { Todos.id eq Uuid.parse(todoId) } > 0
    }

    suspend fun reorder(listId: String, orderedIds: List<String>) = withDb {
        val listUuid = Uuid.parse(listId)
        val now = System.currentTimeMillis()
        orderedIds.forEachIndexed { index, todoId ->
            Todos.update({ (Todos.id eq Uuid.parse(todoId)) and (Todos.listId eq listUuid) }) {
                it[Todos.position] = index.toLong()
                it[Todos.updatedAt] = now
            }
        }
    }
}
