package com.todo.server.auth

import com.todo.server.http.unauthorized
import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import java.time.Duration

data class UserPrincipal(val userId: String) : Principal

/** Cookie carrying the signed JWT session token. */
object SessionCookie {
    const val NAME = "todo_session"
    const val AUTH_PROVIDER = "session"
    const val CSRF_HEADER = "X-CSRF-TOKEN"
    const val CSRF_VALUE = "1"

    fun forToken(token: String, secure: Boolean, maxAge: Duration): Cookie = Cookie(
        name = NAME,
        value = token,
        maxAge = maxAge.toSeconds().toInt(),
        path = "/",
        httpOnly = true,
        secure = secure,
        extensions = mapOf("SameSite" to "Lax"),
    )

    fun clear(secure: Boolean): Cookie = Cookie(
        name = NAME,
        value = "",
        maxAge = 0,
        path = "/",
        httpOnly = true,
        secure = secure,
        extensions = mapOf("SameSite" to "Lax"),
    )
}

/** Current authenticated user id. Only valid inside an `authenticate` route. */
fun ApplicationCall.requireUserId(): String =
    principal<UserPrincipal>()?.userId ?: throw unauthorized()

/** Optional user id (empty when unauthenticated), used by the WebSocket route. */
fun ApplicationCall.sessionUserId(): String? = principal<UserPrincipal>()?.userId
