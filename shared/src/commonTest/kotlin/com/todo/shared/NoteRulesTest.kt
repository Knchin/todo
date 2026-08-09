package com.todo.shared

import com.todo.shared.domain.NoteRules
import com.todo.shared.model.NoteType
import com.todo.shared.validation.Limits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteRulesTest {

    @Test
    fun publicContentReadableWithoutUnlock() {
        assertTrue(NoteRules.canReadContent(NoteType.PUBLIC, unlocked = false))
        assertTrue(NoteRules.canReadContent(NoteType.PUBLIC, unlocked = true))
    }

    @Test
    fun protectedContentRequiresUnlock() {
        assertFalse(NoteRules.canReadContent(NoteType.PROTECTED, unlocked = false))
        assertTrue(NoteRules.canReadContent(NoteType.PROTECTED, unlocked = true))
    }

    @Test
    fun publicNoteMutableByUrlCapability() {
        assertTrue(NoteRules.canMutate(NoteType.PUBLIC, unlocked = false))
    }

    @Test
    fun protectedNoteRequiresUnlockToMutate() {
        assertFalse(NoteRules.canMutate(NoteType.PROTECTED, unlocked = false))
        assertTrue(NoteRules.canMutate(NoteType.PROTECTED, unlocked = true))
    }

    @Test
    fun onlyProtectedRequiresPasscode() {
        assertFalse(NoteRules.requiresPasscode(NoteType.PUBLIC))
        assertTrue(NoteRules.requiresPasscode(NoteType.PROTECTED))
    }

    @Test
    fun generatedPublicIdsAreUniqueAndFixedLength() {
        val ids = (1..500).map { NoteRules.generatePublicId() }
        assertEquals(Limits.PUBLIC_ID_LENGTH, ids.first().length)
        assertEquals(ids.size, ids.toSet().size, "expected all IDs to be unique")
    }

    @Test
    fun base64UrlAlphabetUsed() {
        val id = NoteRules.generatePublicId()
        assertTrue(id.all { it.isLetterOrDigit() || it == '-' || it == '_' })
        assertFalse(id.contains('+'))
        assertFalse(id.contains('/'))
        assertFalse(id.contains('='))
    }
}
