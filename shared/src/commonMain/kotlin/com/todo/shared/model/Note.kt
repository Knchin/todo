package com.todo.shared.model

import kotlinx.serialization.Serializable

/** Visibility of a note. */
@Serializable
enum class NoteType {
    PUBLIC,
    PROTECTED,
}

/**
 * Client-facing note model. This is the only representation of a note exposed
 * to the frontend — it deliberately excludes [passcodeHash] and any internal
 * database fields.
 */
@Serializable
data class Note(
    val id: String,
    val publicId: String,
    val type: NoteType,
    val name: String,
    val description: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Lightweight representation of a public note used on the homepage list.
 * Excludes [Note.content] and any protected-note metadata.
 */
@Serializable
data class NoteSummary(
    val publicId: String,
    val name: String,
    val description: String,
    val updatedAt: Long,
)

/** Payload returned when a requested note is protected and not yet unlocked. */
@Serializable
data class LockedNote(
    val publicId: String,
    val locked: Boolean = true,
    val requiresPasscode: Boolean = true,
)

/** Response of GET note: either a public note, a locked protected note, or a full note after unlock. */
@Serializable
data class NotePayload(
    val note: Note? = null,
    val locked: Boolean = false,
)
