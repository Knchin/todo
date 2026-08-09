package com.todo.shared.domain

import com.todo.shared.model.NoteType
import com.todo.shared.validation.Limits
import kotlin.random.Random

/**
 * Central rules for note capabilities, shared across backend (authoritative)
 * and frontend (UI affordance). The server ALWAYS re-enforces these.
 */
object NoteRules {

    /**
     * A protected note's content is only revealed after successful passcode
     * verification (server-side). Public notes are revealed via their URL.
     */
    fun canReadContent(type: NoteType, unlocked: Boolean): Boolean =
        type == NoteType.PUBLIC || unlocked

    /**
     * Editing/deletion capability: public notes are a capability keyed by the
     * URL's publicId; protected notes additionally require prior unlock.
     */
    fun canMutate(type: NoteType, unlocked: Boolean): Boolean =
        type == NoteType.PUBLIC || (type == NoteType.PROTECTED && unlocked)

    /** Only protected notes ever carry a passcode. */
    fun requiresPasscode(type: NoteType): Boolean = type == NoteType.PROTECTED

    /**
     * Generates a cryptographically-secure public identifier for a note URL.
     * 8 random bytes are base64url-encoded (no padding) to 11 chars; we pad to
     * the configured fixed length for uniform URLs.
     *
     * The browser's crypto RNG is used when available; [Random] here is a
     * reasonable fallback for the server (Deno uses Web Crypto via a shim).
     */
    fun generatePublicId(): String {
        val bytes = ByteArray(12)
        Random.nextBytes(bytes)
        return encodeBase64Url(bytes)
    }

    internal fun encodeBase64Url(bytes: ByteArray): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val sb = StringBuilder(bytes.size * 4 / 3)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2
            sb.append(chars[(triple ushr 18) and 0x3F])
            sb.append(chars[(triple ushr 12) and 0x3F])
            if (i + 1 < bytes.size) sb.append(chars[(triple ushr 6) and 0x3F])
            if (i + 2 < bytes.size) sb.append(chars[triple and 0x3F])
            i += 3
        }
        return sb.toString().take(Limits.PUBLIC_ID_LENGTH)
    }
}
