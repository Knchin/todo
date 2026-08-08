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
            val user = authService.register(request)
            call.response.cookies.append(
                SessionCookie.forToken(authService.tokenFor(user), config.cookieSecure, config.sessionTtl),
            )
            call.respond(HttpStatusCode.Created, AuthResponse(user.toDto()))
        }

        post("/login") {
            if (!rateLimiter.tryAcquire(call.rateLimitKey())) throw rateLimited()
            val request = call.receive<LoginRequest>()
            val user = authService.login(request)
            call.response.cookies.append(
                SessionCookie.forToken(authService.tokenFor(user), config.cookieSecure, config.sessionTtl),
            )
            call.respond(HttpStatusCode.OK, AuthResponse(user.toDto()))
        }

        post("/logout") {
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
