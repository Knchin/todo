package com.todo.server.websocket

import com.todo.server.auth.JwtService
import com.todo.server.auth.SessionCookie
import com.todo.server.database.TodoListRepository
import com.todo.shared.model.ClientMessage
import com.todo.shared.model.ErrorCodes
import com.todo.shared.model.ServerErrorEvent
import com.todo.shared.model.Subscribe
import com.todo.shared.model.Unsubscribe
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json

/**
 * `/ws` realtime endpoint. Authenticates from the session cookie, then
 * accepts subscribe/unsubscribe messages. A client may only subscribe to
 * lists it is a member of.
 */
fun Route.realtimeRoutes(
    jwt: JwtService,
    lists: TodoListRepository,
    hub: RealtimeHub,
    json: Json,
) {
    webSocket("/ws") {
        val userId = call.request.cookies[SessionCookie.NAME]?.let { jwt.verifyToken(it) }
        if (userId == null) {
            send(Frame.Text(error(ErrorCodes.UNAUTHORIZED, "Authentication required.", json)))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
            return@webSocket
        }

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val message = try {
                    json.decodeFromString(ClientMessage.serializer(), frame.readText())
                } catch (_: Exception) {
                    continue
                }
                when (message) {
                    is Subscribe -> {
                        if (lists.roleOf(userId, message.listId) != null) {
                            hub.subscribe(this, message.listId)
                        } else {
                            send(Frame.Text(error(ErrorCodes.FORBIDDEN, "Not a member of this list.", json)))
                        }
                    }

                    is Unsubscribe -> hub.unsubscribe(this, message.listId)
                }
            }
        } finally {
            hub.removeSession(this)
        }
    }
}

private fun error(code: String, message: String, json: Json): String =
    json.encodeToString(ServerErrorEvent.serializer(), ServerErrorEvent(code, message))
