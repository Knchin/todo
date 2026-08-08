package com.todo.shared.domain

import com.todo.shared.model.ListRole

/**
 * Central permission rules shared across backend (authoritative enforcement)
 * and frontend (UI affordance). The server ALWAYS re-checks permissions;
 * the client only uses this to decide which controls to show.
 */
object Permissions {
    /** Every member, including VIEWERs, may view a list and its todos. */
    fun canView(role: ListRole): Boolean = true

    /** EDITORs and OWNERs may create/edit/delete/complete/reorder todos. */
    fun canEditTodos(role: ListRole): Boolean = role != ListRole.VIEWER

    /** Only the OWNER may rename or delete the list. */
    fun canManageList(role: ListRole): Boolean = role == ListRole.OWNER

    /** Only the OWNER may add/remove members or change roles. */
    fun canManageMembers(role: ListRole): Boolean = role == ListRole.OWNER
}
