package com.todo.server.websocket

import com.todo.server.supabase.SupabaseRealtime
import com.todo.shared.model.RealtimeEvent
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Supabase-backed Realtime. Every broadcast is published to a Supabase Realtime
 * channel (`realtime:<listId>`) and also delivered locally to this server's
 * `/ws` sessions. Inbound Supabase events (e.g. from another server instance)
 * are relayed to local `/ws` subscribers, so all clients stay in sync.
 */
class SupabaseRealtimeProvider(
    private val json: Json,
    private val supabase: SupabaseRealtime,
) : Realtime {

    private val listSubscribers = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

    private val inboundHandler: suspend (listId: String, payloadJson: String) -> Unit = { listId, payload ->
        deliverToLocal(listId, payload)
    }

    init {
        supabase.onEvent = inboundHandler
    }

    override fun subscribe(session: DefaultWebSocketServerSession, listId: String) {
        listSubscribers.computeIfAbsent(listId) { ConcurrentHashMap.newKeySet() }.add(session)
        supabase.subscribe(listId)
    }

    override fun unsubscribe(session: DefaultWebSocketServerSession, listId: String) {
        listSubscribers[listId]?.remove(session)
    }

    override fun removeSession(session: DefaultWebSocketServerSession) {
        listSubscribers.values.forEach { it.remove(session) }
    }

    override suspend fun broadcast(listId: String, event: RealtimeEvent) {
        supabase.broadcast(listId, event)
        val payload = json.encodeToString(RealtimeEvent.serializer(), event)
        deliverToLocal(listId, payload)
    }

    private suspend fun deliverToLocal(listId: String, payload: String) {
        val sessions = listSubscribers[listId] ?: return
        if (sessions.isEmpty()) return
        sessions.forEach { session ->
            try {
                session.send(Frame.Text(payload))
            } catch (_: Exception) {
                // Session will be cleaned up when the connection closes.
            }
        }
    }

    override fun close() {
        supabase.close()
    }
}
