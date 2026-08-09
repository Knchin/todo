package com.todo.server

import com.todo.server.auth.AuthProvider
import com.todo.server.auth.JwtService
import com.todo.server.auth.LocalAuthProvider
import com.todo.server.auth.PasswordHasher
import com.todo.server.auth.RateLimiter
import com.todo.server.auth.SessionCookie
import com.todo.server.auth.SupabaseAuthProvider
import com.todo.server.auth.UserPrincipal
import com.todo.server.config.AppConfig
import com.todo.server.database.DatabaseFactory
import com.todo.server.database.TodoListRepository
import com.todo.server.database.TodoRepository
import com.todo.server.database.UserRepository
import com.todo.server.http.ApiException
import com.todo.server.http.unauthorized
import com.todo.server.routes.authenticatedApiRoutes
import com.todo.server.routes.authRoutes
import com.todo.server.routes.staticContent
import com.todo.server.services.AuthService
import com.todo.server.services.ListService
import com.todo.server.services.MemberService
import com.todo.server.services.TodoService
import com.todo.server.supabase.SupabaseAuthClient
import com.todo.server.supabase.SupabaseJwtVerifier
import com.todo.server.supabase.SupabaseRealtime
import com.todo.server.websocket.LocalRealtime
import com.todo.server.websocket.Realtime
import com.todo.server.websocket.SupabaseRealtimeProvider
import com.todo.server.websocket.realtimeRoutes
import com.todo.shared.model.ApiErrorBody
import com.todo.shared.model.ApiErrorEnvelope
import com.todo.shared.model.ErrorCodes
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

fun Application.module(config: AppConfig) {
    DatabaseFactory.init(config)

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    val userRepository = UserRepository()
    val listRepository = TodoListRepository()
    val todoRepository = TodoRepository()

    // HTTP client used for Supabase (Auth REST + JWKS + Realtime socket).
    val supabaseClient = HttpClient(CIO) {
        install(ClientContentNegotiation) { json(json) }
        expectSuccess = false
    }

    val authProvider: AuthProvider
    val hub: Realtime
    if (config.supabaseEnabled) {
        val supabaseAuth = SupabaseAuthClient(config.supabaseUrl, config.supabasePublishableKey, json, supabaseClient)
        val supabaseJwt = SupabaseJwtVerifier(config.supabaseUrl, supabaseClient, json)
        authProvider = SupabaseAuthProvider(userRepository, supabaseAuth, supabaseJwt)
        val supabaseRealtime = SupabaseRealtime(config.supabaseUrl, config.supabasePublishableKey, json, supabaseClient)
        supabaseRealtime.start()
        hub = SupabaseRealtimeProvider(json, supabaseRealtime)
    } else {
        val jwt = JwtService(config)
        val hasher = PasswordHasher()
        authProvider = LocalAuthProvider(userRepository, hasher, jwt)
        hub = LocalRealtime(json)
    }

    val authService = AuthService(authProvider, userRepository)
    val listService = ListService(listRepository, hub)
    val todoService = TodoService(todoRepository, listRepository, listService, hub)
    val memberService = MemberService(listRepository, userRepository, hub)

    val rateLimiter = RateLimiter(config.rateLimitMaxPerWindow, config.rateLimitWindowSeconds.seconds)

    install(ContentNegotiation) {
        json(json)
    }

    install(ForwardedHeaders)

    install(CORS) {
        config.corsAllowedOrigins.forEach { host -> allowHost(host) }
        allowCredentials = true
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(SessionCookie.CSRF_HEADER)
        exposeHeader(HttpHeaders.SetCookie)
    }

    install(CallLogging) {
        level = runCatching { Level.valueOf(config.logLevel.uppercase()) }.getOrDefault(Level.INFO)
    }

    install(WebSockets) {
        pingPeriodMillis = 20_000
        timeoutMillis = 15_000
        maxFrameSize = 16L * 1024
    }

    install(Authentication) {
        provider(SessionCookie.AUTH_PROVIDER) {
            authenticate { context ->
                val userId = context.call.request.cookies[SessionCookie.NAME]
                    ?.let { authProvider.verifyToken(it) }
                    ?: throw unauthorized()
                context.principal(UserPrincipal(userId))
            }
        }
    }

    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(cause.status, ApiErrorEnvelope(ApiErrorBody(cause.code, cause.message)))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiErrorEnvelope(ApiErrorBody(ErrorCodes.VALIDATION, "Invalid request body.")),
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled server error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiErrorEnvelope(ApiErrorBody(ErrorCodes.INTERNAL, "An unexpected error occurred.")),
            )
        }
    }

    routing {
        authRoutes(authService, config, rateLimiter)
        authenticatedApiRoutes(listService, todoService, memberService)
        realtimeRoutes(authProvider, listRepository, hub, json)
        staticContent(config)
    }
}
