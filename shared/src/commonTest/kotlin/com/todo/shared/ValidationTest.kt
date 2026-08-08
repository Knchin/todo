package com.todo.shared

import com.todo.shared.validation.Validation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {
    @Test
    fun validEmailAccepted() {
        assertTrue(Validation.email("a@b.com").valid)
        assertTrue(Validation.email("user.name+tag@example.co.uk").valid)
    }

    @Test
    fun invalidEmailRejected() {
        assertFalse(Validation.email("").valid)
        assertFalse(Validation.email("not-an-email").valid)
        assertFalse(Validation.email("a@b").valid)
        assertFalse(Validation.email("@b.com").valid)
        assertFalse(Validation.email("a@.com").valid)
    }

    @Test
    fun passwordRules() {
        assertFalse(Validation.password("short1").valid)
        assertFalse(Validation.password("allletters").valid)
        assertFalse(Validation.password("12345678").valid)
        assertTrue(Validation.password("password1").valid)
        assertTrue(Validation.password("LongEnoughPassword1").valid)
    }

    @Test
    fun listNameRules() {
        assertFalse(Validation.listName("").valid)
        assertFalse(Validation.listName("   ").valid)
        assertTrue(Validation.listName("Shopping").valid)
    }

    @Test
    fun todoTitleRules() {
        assertFalse(Validation.todoTitle("").valid)
        assertTrue(Validation.todoTitle("Buy milk").valid)
    }
}
