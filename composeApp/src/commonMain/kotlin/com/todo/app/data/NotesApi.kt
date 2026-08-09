package com.todo.app.data

import com.todo.shared.model.ApiErrorEnvelope
import com.todo.shared.model.CreateNoteRequest
import com.todo.shared.model.CreateNoteResponse
import com.todo.shared.model.Note
import com.todo.shared.model.NotePayload
import com.todo.shared.model.NoteSummary
import com.todo.shared.model.PublicNotesResponse
import com.todo.shared.model.UnlockRequest
import com.todo.shared.model.UnlockResponse
import com.todo.shared.model.UpdateNoteRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

sealed class NoteResult<out T> {
    data class Success<T>(val value: T) : NoteResult<T>()
    data class Failure(val code: String, val message: String) : NoteResult<Nothing>()
}

/** HTTP client for Supabase Edge Functions. */
object NotesApi {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
            expectSuccess = false
        }
    }

    private fun base(): String = AppConfig.supabaseUrl.trimEnd('/')
    private fun endpoint(name: String): String = "${base()}/functions/v1/$name"

    private fun io.ktor.client.request.HttpRequestBuilder.applyCommon(token: String? = null) {
        header("apikey", AppConfig.supabaseAnonKey)
        header("Content-Type", ContentType.Application.Json.toString())
        if (token != null) header("Authorization", "Bearer $token")
    }

    suspend fun listPublicNotes(): NoteResult<List<NoteSummary>> {
        val resp = request { url(endpoint("list-public-notes")) }
        return parse(resp) { it.body<PublicNotesResponse>().notes }
    }

    suspend fun getNote(publicId: String): NoteResult<NotePayload> {
        val resp = request {
            url(endpoint("get-note"))
            url.parameters.append("publicId", publicId)
        }
        return parse(resp) { it.body<NotePayload>() }
    }

    suspend fun createNote(request: CreateNoteRequest): NoteResult<CreateNoteResponse> {
        val resp = request {
            url(endpoint("create-note"))
            setBody(request)
        }
        return parse(resp) { it.body<CreateNoteResponse>() }
    }

    suspend fun unlockNote(publicId: String, passcode: String): NoteResult<UnlockResponse> {
        val resp = request {
            url(endpoint("unlock-note"))
            setBody(UnlockRequest(publicId, passcode))
        }
        return parse(resp) { it.body<UnlockResponse>() }
    }

    suspend fun updateNote(request: UpdateNoteRequest, token: String?): NoteResult<Note> {
        val resp = request(token) {
            url(endpoint("update-note"))
            setBody(request)
        }
        return parse(resp) { it.body<NotePayload>().note ?: throw IllegalStateException("empty note") }
    }

    suspend fun deleteNote(publicId: String, token: String?): NoteResult<Unit> {
        val resp = request(token) {
            url(endpoint("delete-note"))
            setBody(mapOf("publicId" to publicId))
        }
        return when (val r = parse<Unit>(resp) { Unit }) {
            is NoteResult.Success -> NoteResult.Success(Unit)
            is NoteResult.Failure -> r
        }
    }

    private suspend fun request(
        token: String? = null,
        build: suspend io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): HttpResponse = try {
        client.post {
            applyCommon(token)
            build()
        }
    } catch (e: Exception) {
        throw e
    }

    private suspend fun <T> parse(
        resp: HttpResponse,
        transform: suspend (HttpResponse) -> T,
    ): NoteResult<T> {
        if (resp.status.isSuccess()) {
            return try {
                NoteResult.Success(transform(resp))
            } catch (e: Exception) {
                NoteResult.Failure("PARSE_ERROR", "Unexpected response format.")
            }
        }
        val envelope = try {
            resp.body<ApiErrorEnvelope>()
        } catch (e: Exception) {
            null
        }
        return NoteResult.Failure(
            envelope?.error?.code ?: "NETWORK_ERROR",
            envelope?.error?.message ?: "Request failed (${resp.status.value})",
        )
    }
}
