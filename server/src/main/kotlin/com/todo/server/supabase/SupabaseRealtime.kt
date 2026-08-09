package com.todo.server.supabase

import com.todo.shared.model.RealtimeEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Publishes domain events to Supabase Realtime channels (topic `realtime:<listId>`)
 * so clients subscribed via Supabase Realtime receive them, and relays any
 * inbound Supabase events back to this server's own `/ws` subscribers.
 *
 * The wire protocol is the Phoenix-style JSON object message used by the
 * Supabase Realtime WebSocket: `{"topic","event","payload","ref"}`. A failed
 * connection is non-fatal: local `/ws` delivery still happens, so the app keeps
 * working even if the Realtime socket drops.
 */
class SupabaseRealtime(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val json: Json,
    private val http: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(SupabaseRealtime::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Invoked for inbound broadcast events: (listId, serialized RealtimeEvent). */
    var onEvent: suspend (listId: String, payloadJson: String) -> Unit = { _, _ -> }

    private val subscribedChannels = ConcurrentHashMap.newKeySet<String>()
    private val sendQueue = Channel<OutgoingMessage>(Channel.UNLIMITED)
    private val refCounter = AtomicLong(1)

    private val wsUrl = buildString {
        append(supabaseUrl.replace("https://", "wss://").replace("http://", "ws://"))
        append("/realtime/v1/websocket?apikey=").append(publishableKey).append("&vsn=1.0.0")
    }

    fun start() {
        scope.launch { runSocket() }
    }

    /** Publishes a domain event to a list's Realtime channel. */
    suspend fun broadcast(listId: String, event: RealtimeEvent) {
        val payload = json.encodeToString(RealtimeEvent.serializer(), event)
        sendQueue.send(
            OutgoingMessage(
                topic = "realtime:$listId",
                event = "broadcast",
                payload = buildJsonObject {
                    put("type", "broadcast")
                    put("event", "todo.event")
                    put("payload", JsonPrimitive(payload))
                },
            ),
        )
    }

    fun subscribe(listId: String) {
        if (subscribedChannels.add(listId)) {
            scope.launch {
                sendQueue.send(joinMessage("realtime:$listId"))
            }
        }
    }

    fun unsubscribe(listId: String) {
        subscribedChannels.remove(listId)
    }

    fun close() {
        scope.cancel()
    }

    private fun joinMessage(topic: String) = OutgoingMessage(
        topic = topic,
        event = "phx_join",
        payload = buildJsonObject {
            put("config", buildJsonObject {})
        },
        ref = nextRef(),
    )

    private fun nextRef(): String = refCounter.incrementAndGet().toString()

    private suspend fun runSocket() {
        while (scope.isActive) {
            try {
                http.webSocket(urlString = wsUrl) {
                    // Phoenix handshake, then re-join channels we care about.
                    sendQueue.send(OutgoingMessage("phoenix", "phoenix", buildJsonObject { put("vsn", "1.0.0") }))
                    subscribedChannels.forEach { sendQueue.send(joinMessage("realtime:$it")) }
                    coroutineScope {
                        launch { drainOutgoing(this@webSocket) }
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            handleIncoming(frame.readText())
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Supabase Realtime connection error: {}", e.message)
            }
            delay(5_000)
        }
    }

    private suspend fun drainOutgoing(session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession) {
        for (message in sendQueue) {
            session.send(Frame.Text(json.encodeToString(OutgoingMessage.serializer(), message)))
        }
    }

    private suspend fun handleIncoming(raw: String) {
        val message = runCatching { json.decodeFromString<IncomingMessage>(raw) }.getOrNull() ?: return
        if (message.event != "broadcast") return
        val payload = message.payload ?: return
        if (payload["type"]?.jsonPrimitive?.content != "broadcast") return
        val eventJson = payload["payload"]?.jsonPrimitive?.content ?: return
        val listId = message.topic.removePrefix("realtime:")
        onEvent(listId, eventJson)
    }
}

@Serializable
data class OutgoingMessage(
    val topic: String,
    val event: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    val ref: String = "",
)

@Serializable
data class IncomingMessage(
    val topic: String = "",
    val event: String = "",
    val payload: JsonObject? = null,
)
