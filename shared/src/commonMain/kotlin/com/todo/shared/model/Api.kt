package com.todo.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateNoteRequest(
    val name: String,
    val description: String = "",
    val content: String,
    val type: NoteType,
    val passcode: String? = null,
)

@Serializable
data class CreateNoteResponse(
    val note: Note,
    val publicUrl: String,
)

@Serializable
data class UnlockRequest(
    val publicId: String,
    val passcode: String,
)

@Serializable
data class UnlockResponse(
    val note: Note,
    val token: String,
)

@Serializable
data class UpdateNoteRequest(
    val publicId: String,
    val name: String,
    val description: String,
    val content: String,
)

@Serializable
data class DeleteNoteRequest(
    val publicId: String,
)

@Serializable
data class PublicNotesResponse(
    val notes: List<NoteSummary>,
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody,
)

/** Stable machine-readable error codes shared by Edge Functions and the client. */
object ErrorCodes {
    const val NOT_FOUND = "NOT_FOUND"
    const val INVALID_REQUEST = "INVALID_REQUEST"
    const val INVALID_PASSCODE = "INVALID_PASSCODE"
    const val FORBIDDEN = "FORBIDDEN"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val INTERNAL = "INTERNAL_ERROR"
    const val OVER_SIZE = "CONTENT_TOO_LARGE"
    const val CONFLICT = "CONFLICT"
}
