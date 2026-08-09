package com.todo.app.state

import com.todo.app.data.Api
import com.todo.app.data.ApiResult
import com.todo.shared.model.TodoDto
import com.todo.shared.model.TodoListDto
import com.todo.shared.model.UpdateTodoRequest
import com.todo.shared.model.UserDto
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object Dashboard : Screen()
    data class TodoList(val listId: String, val listName: String) : Screen()
}

class AppState {
    val scope = CoroutineScope(Dispatchers.Main)

    var currentScreen by mutableStateOf<Screen>(Screen.Login)

    var user: UserDto? by mutableStateOf(null)
        private set
    var lists by mutableStateOf<List<TodoListDto>>(emptyList())
        private set
    var todos by mutableStateOf<Map<String, List<TodoDto>>>(emptyMap())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var sessionInitialized by mutableStateOf(false)
        private set

    fun setScreen(screen: Screen) {
        currentScreen = screen
    }

    fun clearError() {
        error = null
    }

    fun showError(message: String) {
        error = message
    }

    fun login(email: String, password: String, onDone: () -> Unit = {}) {
        scope.launch {
            loading = true
            error = null
            when (val r = Api.login(email.trim(), password)) {
                is ApiResult.Success -> {
                    user = r.value.user
                    sessionInitialized = true
                    currentScreen = Screen.Dashboard
                    refreshLists()
                    onDone()
                }
                is ApiResult.Failure -> error = r.message
            }
            loading = false
        }
    }

    fun register(name: String, email: String, password: String, onDone: () -> Unit = {}) {
        scope.launch {
            loading = true
            error = null
            when (val r = Api.register(name.trim(), email.trim(), password)) {
                is ApiResult.Success -> {
                    user = r.value.user
                    sessionInitialized = true
                    currentScreen = Screen.Dashboard
                    refreshLists()
                    onDone()
                }
                is ApiResult.Failure -> error = r.message
            }
            loading = false
        }
    }

    fun logout() {
        scope.launch {
            Api.logout()
            user = null
            lists = emptyList()
            todos = emptyMap()
            currentScreen = Screen.Login
        }
    }

    fun refreshLists() {
        scope.launch {
            when (val r = Api.myLists()) {
                is ApiResult.Success -> lists = r.value
                is ApiResult.Failure -> if (r.code == "UNAUTHORIZED") currentScreen = Screen.Login
            }
        }
    }

    fun createList(name: String) {
        scope.launch {
            loading = true
            error = null
            when (val r = Api.createList(name.trim())) {
                is ApiResult.Success -> {
                    lists = lists + r.value
                    loading = false
                }
                is ApiResult.Failure -> {
                    error = r.message
                    loading = false
                }
            }
        }
    }

    fun renameList(listId: String, name: String) {
        scope.launch {
            when (val r = Api.renameList(listId, name.trim())) {
                is ApiResult.Success -> {
                    lists = lists.map { if (it.id == listId) r.value else it }
                    if (currentScreen is Screen.TodoList && (currentScreen as Screen.TodoList).listId == listId) {
                        currentScreen = Screen.TodoList(listId, r.value.name)
                    }
                }
                is ApiResult.Failure -> error = r.message
            }
        }
    }

    fun deleteList(listId: String) {
        scope.launch {
            when (val r = Api.deleteList(listId)) {
                is ApiResult.Success -> {
                    lists = lists.filterNot { it.id == listId }
                    if (currentScreen is Screen.TodoList && (currentScreen as Screen.TodoList).listId == listId) {
                        currentScreen = Screen.Dashboard
                    }
                }
                is ApiResult.Failure -> error = r.message
            }
        }
    }

    fun openList(listId: String, listName: String) {
        currentScreen = Screen.TodoList(listId, listName)
        refreshTodos(listId)
    }

    fun todosFor(listId: String): List<TodoDto> = todos[listId] ?: emptyList()

    fun refreshTodos(listId: String) {
        scope.launch {
            when (val r = Api.todosForList(listId)) {
                is ApiResult.Success -> todos = todos + (listId to r.value)
                is ApiResult.Failure -> error = r.message
            }
        }
    }

    fun addTodo(listId: String, title: String, description: String = "") {
        scope.launch {
            when (val r = Api.createTodo(listId, title.trim(), description.trim())) {
                is ApiResult.Success -> {
                    todos = todos + (listId to (todosFor(listId) + r.value))
                }
                is ApiResult.Failure -> error = r.message
            }
        }
    }

    fun toggleTodo(listId: String, todo: TodoDto) {
        scope.launch {
            when (val r = Api.updateTodo(todo.id, UpdateTodoRequest(completed = !todo.completed))) {
                is ApiResult.Success -> {
                    todos = todos + (listId to todosFor(listId).map { if (it.id == todo.id) r.value else it })
                }
                is ApiResult.Failure -> error = r.message
            }
        }
    }

    fun deleteTodo(listId: String, todoId: String) {
        scope.launch {
            when (val r = Api.deleteTodo(todoId)) {
                is ApiResult.Success -> todos = todos + (listId to todosFor(listId).filterNot { it.id == todoId })
                is ApiResult.Failure -> error = r.message
            }
        }
    }
}
