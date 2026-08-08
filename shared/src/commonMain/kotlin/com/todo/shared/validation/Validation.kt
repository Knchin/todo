package com.todo.shared.validation

import com.todo.shared.model.ListRole

/** Result of validating user input. */
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
 * Pure, platform-independent validation rules for all user input.
 * Used by the server (authoritative) and reused by the UI for instant feedback.
 */
object Validation {
    private const val MAX_NAME_LENGTH = 100
    private const val MAX_LIST_NAME_LENGTH = 200
    private const val MAX_TODO_TITLE_LENGTH = 300
    private const val MAX_DESCRIPTION_LENGTH = 10_000
    private const val MIN_PASSWORD_LENGTH = 8
    private const val MAX_PASSWORD_LENGTH = 128

    // Pragmatic email check: non-empty local part, @, non-empty domain with a dot.
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun name(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ValidationResult.fail("Name is required.")
        if (trimmed.length > MAX_NAME_LENGTH) {
            return ValidationResult.fail("Name must be at most $MAX_NAME_LENGTH characters.")
        }
        return ValidationResult.ok()
    }

    fun email(email: String): ValidationResult {
        val trimmed = email.trim().lowercase()
        if (trimmed.isEmpty()) return ValidationResult.fail("Email is required.")
        if (!emailRegex.matches(trimmed)) return ValidationResult.fail("Enter a valid email address.")
        return ValidationResult.ok()
    }

    fun password(password: String): ValidationResult {
        if (password.isEmpty()) return ValidationResult.fail("Password is required.")
        if (password.length < MIN_PASSWORD_LENGTH) {
            return ValidationResult.fail("Password must be at least $MIN_PASSWORD_LENGTH characters.")
        }
        if (password.length > MAX_PASSWORD_LENGTH) {
            return ValidationResult.fail("Password must be at most $MAX_PASSWORD_LENGTH characters.")
        }
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            return ValidationResult.fail("Password must contain at least one letter and one number.")
        }
        return ValidationResult.ok()
    }

    fun listName(name: String): ValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ValidationResult.fail("List name is required.")
        if (trimmed.length > MAX_LIST_NAME_LENGTH) {
            return ValidationResult.fail("List name must be at most $MAX_LIST_NAME_LENGTH characters.")
        }
        return ValidationResult.ok()
    }

    fun todoTitle(title: String): ValidationResult {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return ValidationResult.fail("Todo title is required.")
        if (trimmed.length > MAX_TODO_TITLE_LENGTH) {
            return ValidationResult.fail("Todo title must be at most $MAX_TODO_TITLE_LENGTH characters.")
        }
        return ValidationResult.ok()
    }

    fun description(description: String): ValidationResult {
        if (description.length > MAX_DESCRIPTION_LENGTH) {
            return ValidationResult.fail("Description must be at most $MAX_DESCRIPTION_LENGTH characters.")
        }
        return ValidationResult.ok()
    }

    fun memberRole(role: ListRole?): ValidationResult {
        if (role == null) return ValidationResult.ok()
        if (role == ListRole.OWNER) return ValidationResult.fail("Only the list owner can have the OWNER role.")
        return ValidationResult.ok()
    }

    fun dueDate(dueDate: Long?): ValidationResult = ValidationResult.ok()
}
