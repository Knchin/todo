package com.todo.app.data

import com.todo.shared.model.AddMemberRequest
import com.todo.shared.model.ApiErrorEnvelope
import com.todo.shared.model.AuthResponse
import com.todo.shared.model.CreateListRequest
import com.todo.shared.model.CreateTodoRequest
import com.todo.shared.model.ListRole
import com.todo.shared.model.LoginRequest
import com.todo.shared.model.MemberDto
import com.todo.shared.model.RegisterRequest
import com.todo.shared.model.ReorderTodosRequest
import com.todo.shared.model.TodoDto
import com.todo.shared.model.TodoListDto
import com.todo.shared.model.UpdateListRequest
import com.todo.shared.model.UpdateMemberRoleRequest
import com.todo.shared.model.UpdateTodoRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val code: String, val message: String) : ApiResult<Nothing>()
}

object Api {
    var baseUrl: String = ""
        private set

    fun configureBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            expectSuccess = false
        }
    }

    // --- auth ---

    suspend fun register(name: String, email: String, password: String): ApiResult<AuthResponse> =
        execute {
            url("$baseUrl/api/auth/register")
            method = HttpMethod.Post
            setBody(RegisterRequest(name, email, password))
            contentType(ContentType.Application.Json)
        }

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> =
        execute {
            url("$baseUrl/api/auth/login")
            method = HttpMethod.Post
            setBody(LoginRequest(email, password))
            contentType(ContentType.Application.Json)
        }

    suspend fun logout(): ApiResult<Unit> =
        execute {
            url("$baseUrl/api/auth/logout")
            method = HttpMethod.Post
        }

    // --- lists ---

    suspend fun myLists(): ApiResult<List<TodoListDto>> =
        execute { url("$baseUrl/api/lists") }

    suspend fun getList(listId: String): ApiResult<TodoListDto> =
        execute { url("$baseUrl/api/lists/$listId") }

    suspend fun createList(name: String): ApiResult<TodoListDto> =
        execute {
            url("$baseUrl/api/lists")
            method = HttpMethod.Post
            setBody(CreateListRequest(name))
            contentType(ContentType.Application.Json)
        }

    suspend fun renameList(listId: String, name: String): ApiResult<TodoListDto> =
        execute {
            url("$baseUrl/api/lists/$listId")
            method = HttpMethod.Patch
            setBody(UpdateListRequest(name))
            contentType(ContentType.Application.Json)
        }

    suspend fun deleteList(listId: String): ApiResult<Unit> =
        execute {
            url("$baseUrl/api/lists/$listId")
            method = HttpMethod.Delete
        }

    // --- todos ---

    suspend fun todosForList(listId: String): ApiResult<List<TodoDto>> =
        execute { url("$baseUrl/api/lists/$listId/todos") }

    suspend fun createTodo(listId: String, title: String, description: String): ApiResult<TodoDto> =
        execute {
            url("$baseUrl/api/lists/$listId/todos")
            method = HttpMethod.Post
            setBody(CreateTodoRequest(title = title, description = description))
            contentType(ContentType.Application.Json)
        }

    suspend fun updateTodo(todoId: String, request: UpdateTodoRequest): ApiResult<TodoDto> =
        execute {
            url("$baseUrl/api/todos/$todoId")
            method = HttpMethod.Patch
            setBody(request)
            contentType(ContentType.Application.Json)
        }

    suspend fun deleteTodo(todoId: String): ApiResult<Unit> =
        execute {
            url("$baseUrl/api/todos/$todoId")
            method = HttpMethod.Delete
        }

    suspend fun reorder(listId: String, orderedIds: List<String>): ApiResult<Unit> =
        execute {
            url("$baseUrl/api/lists/$listId/todos/order")
            method = HttpMethod.Put
            setBody(ReorderTodosRequest(orderedIds))
            contentType(ContentType.Application.Json)
        }

    // --- members ---

    suspend fun members(listId: String): ApiResult<List<MemberDto>> =
        execute { url("$baseUrl/api/lists/$listId/members") }

    suspend fun addMember(listId: String, email: String): ApiResult<MemberDto> =
        execute {
            url("$baseUrl/api/lists/$listId/members")
            method = HttpMethod.Post
            setBody(AddMemberRequest(email))
            contentType(ContentType.Application.Json)
        }

    suspend fun updateMemberRole(listId: String, memberId: String, role: ListRole): ApiResult<Unit> =
        execute {
            url("$baseUrl/api/lists/$listId/members/$memberId")
            method = HttpMethod.Patch
            setBody(UpdateMemberRoleRequest(role))
            contentType(ContentType.Application.Json)
        }

    suspend fun removeMember(listId: String, memberId: String): ApiResult<Unit> =
        execute {
            url("$baseUrl/api/lists/$listId/members/$memberId")
            method = HttpMethod.Delete
        }

    private suspend inline fun <reified T> execute(
        crossinline build: HttpRequestBuilder.() -> Unit,
    ): ApiResult<T> {
        val response: HttpResponse = try {
            httpClient.request {
                build()
                csrfHeader()
            }
        } catch (e: Exception) {
            return ApiResult.Failure("NETWORK_ERROR", e.message ?: "Network error")
        }
        return if (response.status.isSuccess()) {
            try {
                @Suppress("UNCHECKED_CAST")
                if (T::class == Unit::class) {
                    ApiResult.Success(Unit as T)
                } else {
                    ApiResult.Success(response.body())
                }
            } catch (e: Exception) {
                ApiResult.Failure("PARSE_ERROR", "Could not parse response")
            }
        } else {
            parseError(response)
        }
    }

    private fun HttpRequestBuilder.csrfHeader() {
        if (method in CSRF_PROTECTED_METHODS) {
            header(CSRF_HEADER, CSRF_VALUE)
        }
    }

    private suspend fun <T> parseError(response: HttpResponse): ApiResult<T> {
        return try {
            val envelope = response.body<ApiErrorEnvelope>()
            ApiResult.Failure(envelope.error.code, envelope.error.message)
        } catch (e: Exception) {
            ApiResult.Failure("UNKNOWN_ERROR", "Request failed (${response.status.value})")
        }
    }

    private val CSRF_PROTECTED_METHODS = setOf(
        HttpMethod.Post,
        HttpMethod.Put,
        HttpMethod.Patch,
        HttpMethod.Delete,
    )

    private const val CSRF_HEADER = "X-CSRF-TOKEN"
    private const val CSRF_VALUE = "1"
}
