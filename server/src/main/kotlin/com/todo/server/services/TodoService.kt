package com.todo.server.services

import com.todo.server.database.TodoListRepository
import com.todo.server.database.TodoRepository
import com.todo.server.http.notFound
import com.todo.server.http.validationError
import com.todo.server.websocket.RealtimeHub
import com.todo.shared.model.CreateTodoRequest
import com.todo.shared.model.ReorderTodosRequest
import com.todo.shared.model.TodoCreated
import com.todo.shared.model.TodoCompleted
import com.todo.shared.model.TodoDeleted
import com.todo.shared.model.TodoDto
import com.todo.shared.model.TodoReordered
import com.todo.shared.model.TodoUpdated
import com.todo.shared.model.UpdateTodoRequest
import com.todo.shared.validation.Validation

class TodoService(
    private val todos: TodoRepository,
    private val lists: TodoListRepository,
    private val listService: ListService,
    private val hub: RealtimeHub,
) {
    suspend fun listForList(userId: String, listId: String): List<TodoDto> {
        listService.ensureMember(userId, listId)
        return todos.todosForList(listId)
    }

    suspend fun create(userId: String, listId: String, request: CreateTodoRequest): TodoDto {
        listService.ensureCanEdit(userId, listId)
        validateCreate(request, listId)
        val todo = todos.create(
            listId = listId,
            createdBy = userId,
            title = request.title.trim(),
            description = request.description,
            dueDate = request.dueDate,
            assignedTo = request.assignedTo,
        )
        hub.broadcast(listId, TodoCreated(todo))
        return todo
    }

    suspend fun update(userId: String, todoId: String, request: UpdateTodoRequest): TodoDto {
        val existing = todos.findById(todoId) ?: throw notFound("Todo not found.")
        listService.ensureCanEdit(userId, existing.listId)
        validateUpdate(request, existing.listId)

        val updated = todos.update(todoId, request) ?: throw notFound("Todo not found.")
        val completedToggled = request.completed != null && request.completed != existing.completed
        if (completedToggled) {
            hub.broadcast(existing.listId, TodoCompleted(updated))
        } else {
            hub.broadcast(existing.listId, TodoUpdated(updated))
        }
        return updated
    }

    suspend fun delete(userId: String, todoId: String) {
        val existing = todos.findById(todoId) ?: throw notFound("Todo not found.")
        listService.ensureCanEdit(userId, existing.listId)
        if (!todos.delete(todoId)) throw notFound("Todo not found.")
        hub.broadcast(existing.listId, TodoDeleted(todoId, existing.listId))
    }

    suspend fun reorder(userId: String, listId: String, request: ReorderTodosRequest) {
        listService.ensureCanEdit(userId, listId)
        val current = todos.todosForList(listId)
        val currentIds = current.map { it.id }.toSet()
        if (request.orderedIds.toSet() != currentIds) {
            throw validationError("Reorder must include exactly the todos of this list.")
        }
        todos.reorder(listId, request.orderedIds)
        hub.broadcast(listId, TodoReordered(request.orderedIds, listId))
    }

    private suspend fun validateCreate(request: CreateTodoRequest, listId: String) {
        val title = Validation.todoTitle(request.title)
        if (!title.valid) throw validationError(title.error!!)
        val description = Validation.description(request.description)
        if (!description.valid) throw validationError(description.error!!)
        request.assignedTo?.let { requireMember(listId, it) }
    }

    private suspend fun validateUpdate(request: UpdateTodoRequest, listId: String) {
        request.title?.let {
            val title = Validation.todoTitle(it)
            if (!title.valid) throw validationError(title.error!!)
        }
        request.description?.let {
            val description = Validation.description(it)
            if (!description.valid) throw validationError(description.error!!)
        }
        if (!request.clearAssignedTo) {
            request.assignedTo?.let { requireMember(listId, it) }
        }
    }

    private suspend fun requireMember(listId: String, userId: String) {
        if (!lists.isMember(listId, userId)) {
            throw validationError("The assignee must be a member of this list.")
        }
    }
}
