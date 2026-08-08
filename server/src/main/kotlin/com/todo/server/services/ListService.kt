package com.todo.server.services

import com.todo.server.database.TodoListRepository
import com.todo.server.http.forbidden
import com.todo.server.http.notFound
import com.todo.server.http.validationError
import com.todo.server.websocket.RealtimeHub
import com.todo.shared.domain.Permissions
import com.todo.shared.model.ListRole
import com.todo.shared.model.CreateListRequest
import com.todo.shared.model.TodoListDto
import com.todo.shared.model.UpdateListRequest
import com.todo.shared.model.ListDeleted
import com.todo.shared.model.ListUpdated
import com.todo.shared.validation.Validation

class ListService(
    private val lists: TodoListRepository,
    private val hub: RealtimeHub,
) {
    suspend fun myLists(userId: String): List<TodoListDto> = lists.listsForUser(userId)

    suspend fun create(userId: String, request: CreateListRequest): TodoListDto {
        val result = Validation.listName(request.name)
        if (!result.valid) throw validationError(result.error!!)
        return lists.create(userId, request.name.trim())
    }

    suspend fun get(userId: String, listId: String): TodoListDto {
        return lists.findForUser(userId, listId) ?: throw notFound("List not found.")
    }

    suspend fun rename(userId: String, listId: String, request: UpdateListRequest): TodoListDto {
        val result = Validation.listName(request.name)
        if (!result.valid) throw validationError(result.error!!)
        ensureRole(userId, listId, Permissions::canManageList, "Only the owner can rename this list.")

        if (!lists.rename(listId, request.name.trim())) throw notFound("List not found.")
        val updated = lists.findForUser(userId, listId) ?: throw notFound("List not found.")
        hub.broadcast(listId, ListUpdated(updated))
        return updated
    }

    suspend fun delete(userId: String, listId: String) {
        ensureRole(userId, listId, Permissions::canManageList, "Only the owner can delete this list.")
        if (!lists.delete(listId)) throw notFound("List not found.")
        hub.broadcast(listId, ListDeleted(listId))
    }

    suspend fun ensureMember(userId: String, listId: String): ListRole {
        return lists.roleOf(userId, listId) ?: throw notFound("List not found.")
    }

    suspend fun ensureCanEdit(userId: String, listId: String): ListRole {
        val role = ensureMember(userId, listId)
        if (!Permissions.canEditTodos(role)) {
            throw forbidden("You do not have permission to modify this list.")
        }
        return role
    }

    private suspend fun ensureRole(
        userId: String,
        listId: String,
        check: (ListRole) -> Boolean,
        message: String,
    ) {
        val role = lists.roleOf(userId, listId) ?: throw notFound("List not found.")
        if (!check(role)) throw forbidden(message)
    }
}
