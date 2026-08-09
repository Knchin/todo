package com.todo.server.routes

import com.todo.server.auth.RateLimiter
import com.todo.server.auth.SessionCookie
import com.todo.server.config.AppConfig
import com.todo.server.database.UserRecord
import com.todo.server.http.rateLimited
import com.todo.server.services.AuthService
import com.todo.shared.model.AuthResponse
import com.todo.shared.model.LoginRequest
import com.todo.shared.model.RegisterRequest
import com.todo.shared.model.UserDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(
    authService: AuthService,
    config: AppConfig,
    rateLimiter: RateLimiter,
) {
    route("/api/auth") {
        post("/register") {
            if (!rateLimiter.tryAcquire(call.rateLimitKey())) throw rateLimited()
            val request = call.receive<RegisterRequest>()
            val result = authService.register(request)
            call.response.cookies.append(
                SessionCookie.forToken(result.token, config.cookieSecure, config.sessionTtl),
            )
            call.respond(HttpStatusCode.Created, AuthResponse(result.user.toDto()))
        }

        post("/login") {
            if (!rateLimiter.tryAcquire(call.rateLimitKey())) throw rateLimited()
            val request = call.receive<LoginRequest>()
            val result = authService.login(request)
            call.response.cookies.append(
                SessionCookie.forToken(result.token, config.cookieSecure, config.sessionTtl),
            )
            call.respond(HttpStatusCode.OK, AuthResponse(result.user.toDto()))
        }

        post("/logout") {
            val token = call.request.cookies[SessionCookie.NAME].orEmpty()
            authService.logout(token)
            call.response.cookies.append(SessionCookie.clear(config.cookieSecure))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun ApplicationCall.rateLimitKey(): String = request.origin.remoteHost

fun UserRecord.toDto() = UserDto(
    id = id,
    name = name,
    email = email,
    createdAt = createdAt,
)
