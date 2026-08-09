package com.todo.shared.validation

import com.todo.shared.model.NoteType

data class ValidationResult(
    val valid: Boolean,
    val error: String? = null,
) {
    companion object {
        fun ok() = ValidationResult(valid = true)
        fun fail(message: String) = ValidationResult(valid = false, error = message)
    }
}

/**
 * Platform-independent limits and validation for anonymous notes. The server
 * (Edge Function) enforces these authoritatively; the UI reuses them for
 * instant feedback. Units are characters for text fields.
 */
object Limits {
    const val MAX_NAME_LENGTH = 200
    const val MAX_DESCRIPTION_LENGTH = 2_000
    const val MAX_CONTENT_LENGTH = 20_000
    const val MIN_PASSCODE_LENGTH = 4
    const val MAX_PASSCODE_LENGTH = 128
    const val PUBLIC_ID_LENGTH = 16
}

object NoteValidator {

    fun name(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ValidationResult.fail("Name is required.")
        if (trimmed.length > Limits.MAX_NAME_LENGTH) {
            return ValidationResult.fail("Name must be at most ${Limits.MAX_NAME_LENGTH} characters.")
        }
        return ValidationResult.ok()
    }

    fun description(description: String): ValidationResult {
        if (description.length > Limits.MAX_DESCRIPTION_LENGTH) {
            return ValidationResult.fail(
                "Description must be at most ${Limits.MAX_DESCRIPTION_LENGTH} characters.",
            )
        }
        return ValidationResult.ok()
    }

    fun content(content: String): ValidationResult {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return ValidationResult.fail("Content is required.")
        if (trimmed.length > Limits.MAX_CONTENT_LENGTH) {
            return ValidationResult.fail("Content must be at most ${Limits.MAX_CONTENT_LENGTH} characters.")
        }
        return ValidationResult.ok()
    }

    fun passcode(passcode: String): ValidationResult {
        if (passcode.isEmpty()) return ValidationResult.fail("A passcode is required for protected notes.")
        if (passcode.length < Limits.MIN_PASSCODE_LENGTH) {
            return ValidationResult.fail(
                "Passcode must be at least ${Limits.MIN_PASSCODE_LENGTH} characters.",
            )
        }
        if (passcode.length > Limits.MAX_PASSCODE_LENGTH) {
            return ValidationResult.fail("Passcode must be at most ${Limits.MAX_PASSCODE_LENGTH} characters.")
        }
        return ValidationResult.ok()
    }

    /**
     * Validates a full note creation payload. For PROTECTED notes the passcode
     * is mandatory; for PUBLIC notes it must be absent.
     */
    fun create(
        name: String,
        description: String,
        content: String,
        type: NoteType,
        passcode: String?,
    ): ValidationResult {
        name(name).let { if (!it.valid) return it }
        description(description).let { if (!it.valid) return it }
        content(content).let { if (!it.valid) return it }
        return when (type) {
            NoteType.PUBLIC -> {
                if (!passcode.isNullOrBlank()) {
                    ValidationResult.fail("A public note cannot have a passcode.")
                } else {
                    ValidationResult.ok()
                }
            }
            NoteType.PROTECTED -> passcode(passcode.orEmpty())
        }
    }
}
