package com.todo.server.websocket

import com.todo.shared.model.RealtimeEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * In-process fan-out: broadcasts events to every `/ws` session subscribed to
 * the affected list. Used for local dev and the embedded-Postgres test suite.
 */
class LocalRealtime(private val json: Json) : Realtime {
    private val listSubscribers = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

    override fun subscribe(session: DefaultWebSocketServerSession, listId: String) {
        listSubscribers.computeIfAbsent(listId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    override fun unsubscribe(session: DefaultWebSocketServerSession, listId: String) {
        listSubscribers[listId]?.remove(session)
    }

    override fun removeSession(session: DefaultWebSocketServerSession) {
        listSubscribers.values.forEach { it.remove(session) }
    }

    override suspend fun broadcast(listId: String, event: RealtimeEvent) {
        val sessions = listSubscribers[listId] ?: return
        if (sessions.isEmpty()) return
        val payload = json.encodeToString(RealtimeEvent.serializer(), event)
        sessions.forEach { session ->
            try {
                session.send(Frame.Text(payload))
            } catch (_: Exception) {
                // Session will be cleaned up when the connection closes.
            }
        }
    }

    override fun close() = Unit
}
