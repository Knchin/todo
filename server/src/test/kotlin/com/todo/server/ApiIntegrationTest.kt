package com.todo.server

import com.todo.server.config.AppConfig
import com.todo.server.database.DatabaseFactory
import com.todo.shared.model.AddMemberRequest
import com.todo.shared.model.ApiErrorEnvelope
import com.todo.shared.model.AuthResponse
import com.todo.shared.model.CreateListRequest
import com.todo.shared.model.CreateTodoRequest
import com.todo.shared.model.ListRole
import com.todo.shared.model.LoginRequest
import com.todo.shared.model.MemberDto
import com.todo.shared.model.ReorderTodosRequest
import com.todo.shared.model.RegisterRequest
import com.todo.shared.model.TodoDto
import com.todo.shared.model.TodoListDto
import com.todo.shared.model.UpdateListRequest
import com.todo.shared.model.UpdateMemberRoleRequest
import com.todo.shared.model.UpdateTodoRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val CSRF_HEADER = "X-CSRF-TOKEN"
private const val CSRF_VALUE = "1"

fun HttpRequestBuilder.csrf() {
    header(CSRF_HEADER, CSRF_VALUE)
}

inline fun <reified T> HttpRequestBuilder.jsonBody(body: T, json: Json) {
    contentType(ContentType.Application.Json)
    setBody(json.encodeToString(body))
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private lateinit var postgres: EmbeddedPostgres
    private lateinit var dataSource: DataSource
    private lateinit var config: AppConfig

    @BeforeAll
    fun startDatabase() {
        postgres = EmbeddedPostgres.start()
        dataSource = postgres.postgresDatabase
        DatabaseFactory.migrate(dataSource)
        config = AppConfig(
            host = "localhost",
            port = 0,
            dbUrl = postgres.getJdbcUrl("postgres", "postgres"),
            dbUser = "postgres",
            dbPassword = "postgres",
            dbMaxPoolSize = 3,
            jwtSecret = "test-secret-that-is-at-least-32-chars-long!!",
            jwtIssuer = "todo-kmp-test",
            sessionTtl = Duration.ofDays(1),
            cookieSecure = false,
            corsAllowedOrigins = emptyList(),
            staticRoot = null,
            rateLimitMaxPerWindow = 100,
            rateLimitWindowSeconds = 60,
            logLevel = "ERROR",
        )
    }

    @AfterAll
    fun stopDatabase() {
        postgres.close()
    }

    @BeforeEach
    fun cleanDatabase() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                st.execute("TRUNCATE todos, list_members, todo_lists, users CASCADE")
            }
        }
    }

    private fun withApi(block: suspend ApiTestClient.() -> Unit) {
        testApplication {
            application { module(config) }
            val client = createClient { install(HttpCookies) }
            ApiTestClient(client, json).block()
        }
    }

    @Test
    fun `register logs in and logs out`() = withApi {
        val register = register("Alice", "alice@example.com", "password1")
        assertEquals(HttpStatusCode.Created, register.status)
        val auth = register.decode<AuthResponse>()
        assertEquals("alice@example.com", auth.user.email)
        assertEquals("Alice", auth.user.name)
        assertNotNull(auth.user.id)

        assertEquals(HttpStatusCode.OK, get("/api/lists").status)
        assertEquals(HttpStatusCode.NoContent, post("/api/auth/logout").status)
        assertEquals(HttpStatusCode.Unauthorized, get("/api/lists").status)
    }

    @Test
    fun `login with wrong password is rejected`() = withApi {
        register("Bob", "bob@example.com", "password1")
        assertEquals(HttpStatusCode.Unauthorized, login("bob@example.com", "wrong-password").status)
    }

    @Test
    fun `unauthenticated requests are rejected`() = withApi {
        assertEquals(HttpStatusCode.Unauthorized, get("/api/lists").status)
    }

    @Test
    fun `duplicate registration conflicts`() = withApi {
        register("Cara", "cara@example.com", "password1")
        val second = register("Cara", "cara@example.com", "password1")
        assertEquals(HttpStatusCode.Conflict, second.status)
        assertEquals("CONFLICT", second.decode<ApiErrorEnvelope>().error.code)
    }

    @Test
    fun `validation errors return 400`() = withApi {
        assertEquals(HttpStatusCode.BadRequest, register("Dan", "not-an-email", "password1").status)
        assertEquals(HttpStatusCode.BadRequest, register("Dan", "dan@example.com", "short1").status)

        register("Dan", "dan@example.com", "password1")
        assertEquals(
            HttpStatusCode.BadRequest,
            post("/api/lists") { jsonBody(CreateListRequest("   "), json) }.status,
        )

        val listId = createList("Work").id
        assertEquals(
            HttpStatusCode.BadRequest,
            post("/api/lists/$listId/todos") { jsonBody(CreateTodoRequest(" ", ""), json) }.status,
        )
    }

    @Test
    fun `list lifecycle`() = withApi {
        register("Eve", "eve@example.com", "password1")
        val created = createList("Shopping")
        assertEquals("Shopping", created.name)
        assertEquals(ListRole.OWNER, created.role)

        val fetched = get("/api/lists/${created.id}").decode<TodoListDto>()
        assertEquals("Shopping", fetched.name)
        assertEquals(1, fetched.members.size)

        val renamed = patch("/api/lists/${created.id}") { jsonBody(UpdateListRequest("Groceries"), json) }
            .decode<TodoListDto>()
        assertEquals("Groceries", renamed.name)

        val mine = get("/api/lists").decode<List<TodoListDto>>()
        assertEquals(listOf("Groceries"), mine.map { it.name })

        assertEquals(HttpStatusCode.NoContent, delete("/api/lists/${created.id}").status)
        assertEquals(emptyList(), get("/api/lists").decode<List<TodoListDto>>())
    }

    @Test
    fun `todo lifecycle`() = withApi {
        register("Frank", "frank@example.com", "password1")
        val listId = createList("Plan").id

        val todo = createTodo(listId, "Write docs", "Some description")
        assertEquals("Write docs", todo.title)
        assertFalse(todo.completed)
        assertEquals("Frank", todo.createdByName)

        val todos = get("/api/lists/$listId/todos").decode<List<TodoDto>>()
        assertEquals(listOf("Write docs"), todos.map { it.title })

        val updated = patch("/api/todos/${todo.id}") {
            jsonBody(UpdateTodoRequest(title = "Rewrite docs", completed = true), json)
        }.decode<TodoDto>()
        assertEquals("Rewrite docs", updated.title)
        assertTrue(updated.completed)

        val second = createTodo(listId, "Second")
        assertEquals(2L, second.position)
        put("/api/lists/$listId/todos/order") {
            jsonBody(ReorderTodosRequest(listOf(second.id, updated.id)), json)
        }
        val reordered = get("/api/lists/$listId/todos").decode<List<TodoDto>>()
        assertEquals(listOf("Second", "Rewrite docs"), reordered.map { it.title })
        assertEquals(listOf(0L, 1L), reordered.map { it.position })

        assertEquals(HttpStatusCode.NoContent, delete("/api/todos/${updated.id}").status)
        assertEquals(listOf("Second"), get("/api/lists/$listId/todos").decode<List<TodoDto>>().map { it.title })
    }

    @Test
    fun `members can be added updated and removed`() = withApi {
        register("Heidi", "heidi@example.com", "password1")
        register("Grace", "grace@example.com", "password1")
        val listId = createList("Team").id

        val added = post("/api/lists/$listId/members") {
            jsonBody(AddMemberRequest("heidi@example.com", ListRole.EDITOR), json)
        }
        assertEquals(HttpStatusCode.Created, added.status)

        val members = get("/api/lists/$listId/members").decode<List<MemberDto>>()
        val heidi = members.single { it.email == "heidi@example.com" }
        assertEquals(ListRole.EDITOR, heidi.role)

        patch("/api/lists/$listId/members/${heidi.userId}") {
            jsonBody(UpdateMemberRoleRequest(ListRole.VIEWER), json)
        }
        assertEquals(
            ListRole.VIEWER,
            get("/api/lists/$listId/members")
                .decode<List<MemberDto>>()
                .single { it.email == "heidi@example.com" }
                .role,
        )

        assertEquals(HttpStatusCode.NoContent, delete("/api/lists/$listId/members/${heidi.userId}").status)
        assertEquals(
            listOf("grace@example.com"),
            get("/api/lists/$listId/members").decode<List<MemberDto>>().map { it.email },
        )
    }

    @Test
    fun `viewer cannot edit todos`() = withApi {
        register("Wendy", "wendy@example.com", "password1")
        register("Vince", "vince@example.com", "password1")
        val listId = createList("ViewOnly").id
        val todo = createTodo(listId, "Owner todo")

        post("/api/lists/$listId/members") {
            jsonBody(AddMemberRequest("wendy@example.com", ListRole.VIEWER), json)
        }
        login("wendy@example.com", "password1")

        assertEquals(HttpStatusCode.Forbidden, patch("/api/todos/${todo.id}") {
            jsonBody(UpdateTodoRequest(completed = true), json)
        }.status)
    }

    @Test
    fun `editor can edit todos but not manage members`() = withApi {
        register("Liam", "liam@example.com", "password1")
        register("Kyle", "kyle@example.com", "password1")
        val listId = createList("Collab").id
        post("/api/lists/$listId/members") {
            jsonBody(AddMemberRequest("liam@example.com", ListRole.EDITOR), json)
        }

        val todoId = createTodo(listId, "Editor task").id
        login("liam@example.com", "password1")
        assertEquals(HttpStatusCode.OK, patch("/api/todos/$todoId") {
            jsonBody(UpdateTodoRequest(title = "Edited by editor"), json)
        }.status)

        assertEquals(HttpStatusCode.Forbidden, post("/api/lists/$listId/members") {
            jsonBody(AddMemberRequest("someone@example.com", ListRole.VIEWER), json)
        }.status)
    }

    @Test
    fun `non-owner cannot delete the list`() = withApi {
        register("Oscar", "oscar@example.com", "password1")
        register("Nick", "nick@example.com", "password1")
        val listId = createList("Protected").id
        post("/api/lists/$listId/members") {
            jsonBody(AddMemberRequest("oscar@example.com", ListRole.EDITOR), json)
        }
        login("oscar@example.com", "password1")

        assertEquals(HttpStatusCode.Forbidden, delete("/api/lists/$listId").status)
        assertEquals(HttpStatusCode.OK, get("/api/lists/$listId").status)
    }

    @Test
    fun `CSRF header is required for mutating requests`() = withApi {
        register("Mia", "mia@example.com", "password1")

        val noCsrf = postNoCsrf("/api/lists") { jsonBody(CreateListRequest("Sneaky"), json) }
        assertEquals(HttpStatusCode.Forbidden, noCsrf.status)

        val withCsrf = post("/api/lists") { jsonBody(CreateListRequest("Legit"), json) }
        assertEquals(HttpStatusCode.Created, withCsrf.status)
    }

    @Test
    fun `todo not found returns 404`() = withApi {
        register("Pat", "pat@example.com", "password1")
        assertEquals(HttpStatusCode.NotFound, delete("/api/todos/${java.util.UUID.randomUUID()}").status)
    }

    // --- helpers -------------------------------------------------------

    class ApiTestClient(
        val client: HttpClient,
        val json: Json,
    ) {
        suspend fun register(name: String, email: String, password: String): HttpResponse =
            jsonPost("/api/auth/register", RegisterRequest(name, email, password))

        suspend fun login(email: String, password: String): HttpResponse =
            jsonPost("/api/auth/login", LoginRequest(email, password))

        suspend fun createList(name: String): TodoListDto =
            jsonPost("/api/lists", CreateListRequest(name)).decode()

        suspend fun createTodo(listId: String, title: String, description: String = ""): TodoDto =
            jsonPost("/api/lists/$listId/todos", CreateTodoRequest(title, description)).decode()

        suspend fun get(path: String): HttpResponse = client.get(path)

        suspend inline fun <reified T> jsonPost(path: String, body: T): HttpResponse =
            client.post(path) {
                csrf()
                jsonBody(body, json)
            }

        suspend fun post(path: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
            client.post(path) {
                csrf()
                block()
            }

        suspend fun postNoCsrf(path: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
            client.post(path) { block() }

        suspend fun patch(path: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
            client.patch(path) {
                csrf()
                block()
            }

        suspend fun put(path: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
            client.put(path) {
                csrf()
                block()
            }

        suspend fun delete(path: String): HttpResponse = client.delete(path) { csrf() }

        suspend inline fun <reified T> HttpResponse.decode(): T = json.decodeFromString(bodyAsText())
    }
}
