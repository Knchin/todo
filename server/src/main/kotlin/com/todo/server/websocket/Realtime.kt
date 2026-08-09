package com.todo.server.websocket

import com.todo.shared.model.RealtimeEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession

/**
 * Fan-out of [RealtimeEvent]s to subscribers of a todo list. Implementations
 * may broadcast to this server's `/ws` sessions, to Supabase Realtime channels,
 * or both.
 */
interface Realtime {
    fun subscribe(session: DefaultWebSocketServerSession, listId: String)
    fun unsubscribe(session: DefaultWebSocketServerSession, listId: String)
    fun removeSession(session: DefaultWebSocketServerSession)
    suspend fun broadcast(listId: String, event: RealtimeEvent)
    fun close()
}
