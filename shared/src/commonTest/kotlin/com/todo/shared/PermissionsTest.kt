package com.todo.shared

import com.todo.shared.model.ListRole
import com.todo.shared.domain.Permissions
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionsTest {
    @Test
    fun everyRoleCanView() {
        assertTrue(Permissions.canView(ListRole.OWNER))
        assertTrue(Permissions.canView(ListRole.EDITOR))
        assertTrue(Permissions.canView(ListRole.VIEWER))
    }

    @Test
    fun viewerCannotEditTodos() {
        assertFalse(Permissions.canEditTodos(ListRole.VIEWER))
        assertTrue(Permissions.canEditTodos(ListRole.EDITOR))
        assertTrue(Permissions.canEditTodos(ListRole.OWNER))
    }

    @Test
    fun onlyOwnerManagesList() {
        assertTrue(Permissions.canManageList(ListRole.OWNER))
        assertFalse(Permissions.canManageList(ListRole.EDITOR))
        assertFalse(Permissions.canManageList(ListRole.VIEWER))
    }

    @Test
    fun onlyOwnerManagesMembers() {
        assertTrue(Permissions.canManageMembers(ListRole.OWNER))
        assertFalse(Permissions.canManageMembers(ListRole.EDITOR))
        assertFalse(Permissions.canManageMembers(ListRole.VIEWER))
    }
}
