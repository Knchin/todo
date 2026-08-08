package com.todo.server.websocket

import com.todo.shared.model.RealtimeEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks WebSocket sessions per todo list and fans out strongly-typed events
 * to every subscriber of that list. Send failures are swallowed and cleaned
 * up on disconnect.
 */
class RealtimeHub(private val json: Json) {
    private val listSubscribers = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

    fun subscribe(session: DefaultWebSocketServerSession, listId: String) {
        listSubscribers.computeIfAbsent(listId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unsubscribe(session: DefaultWebSocketServerSession, listId: String) {
        listSubscribers[listId]?.remove(session)
    }

    fun removeSession(session: DefaultWebSocketServerSession) {
        listSubscribers.values.forEach { it.remove(session) }
    }

    suspend fun broadcast(listId: String, event: RealtimeEvent) {
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
}
