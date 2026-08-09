package com.todo.shared

import com.todo.shared.model.NoteType
import com.todo.shared.validation.NoteValidator
import com.todo.shared.validation.ValidationResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class NoteValidatorTest {

    private fun assertInvalid(result: ValidationResult) {
        assertFalse(result.valid, "expected invalid but got: ${result.error}")
        assertNotNull(result.error)
    }

    private fun assertValid(result: ValidationResult) {
        assertTrue(result.valid, "expected valid but got: ${result.error}")
        assertNull(result.error)
    }

    @Test
    fun validPublicNote() {
        assertValid(NoteValidator.create("Shopping", "grocery", "milk", NoteType.PUBLIC, null))
    }

    @Test
    fun publicNoteCannotHavePasscode() {
        assertInvalid(NoteValidator.create("Shopping", "", "milk", NoteType.PUBLIC, "1234"))
    }

    @Test
    fun protectedNoteRequiresPasscode() {
        assertInvalid(NoteValidator.create("Secret", "", "milk", NoteType.PROTECTED, null))
        assertInvalid(NoteValidator.create("Secret", "", "milk", NoteType.PROTECTED, ""))
    }

    @Test
    fun validProtectedNote() {
        assertValid(NoteValidator.create("Secret", "", "milk", NoteType.PROTECTED, "correct-horse"))
    }

    @Test
    fun passcodeTooShort() {
        assertInvalid(NoteValidator.create("Secret", "", "milk", NoteType.PROTECTED, "12"))
    }

    @Test
    fun passcodeTooLong() {
        val long = "a".repeat(200)
        assertInvalid(NoteValidator.create("Secret", "", "milk", NoteType.PROTECTED, long))
    }

    @Test
    fun emptyNameRejected() {
        assertInvalid(NoteValidator.create("   ", "", "milk", NoteType.PUBLIC, null))
    }

    @Test
    fun nameTooLong() {
        assertInvalid(NoteValidator.create("n".repeat(300), "", "milk", NoteType.PUBLIC, null))
    }

    @Test
    fun emptyContentRejected() {
        assertInvalid(NoteValidator.create("Shopping", "", "   ", NoteType.PUBLIC, null))
    }

    @Test
    fun contentTooLong() {
        assertInvalid(NoteValidator.create("Shopping", "", "x".repeat(30_000), NoteType.PUBLIC, null))
    }

    @Test
    fun descriptionTooLong() {
        assertInvalid(NoteValidator.create("Shopping", "d".repeat(5_000), "milk", NoteType.PUBLIC, null))
    }

    @Test
    fun emptyDescriptionAllowed() {
        assertValid(NoteValidator.create("Shopping", "", "milk", NoteType.PUBLIC, null))
    }
}
