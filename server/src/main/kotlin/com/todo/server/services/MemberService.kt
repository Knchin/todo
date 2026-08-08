package com.todo.server.services

import com.todo.server.database.TodoListRepository
import com.todo.server.database.UserRepository
import com.todo.server.http.conflict
import com.todo.server.http.forbidden
import com.todo.server.http.notFound
import com.todo.server.http.validationError
import com.todo.server.websocket.RealtimeHub
import com.todo.shared.model.AddMemberRequest
import com.todo.shared.model.ListRole
import com.todo.shared.model.MemberAdded
import com.todo.shared.model.MemberDto
import com.todo.shared.model.MemberRemoved
import com.todo.shared.model.MemberRoleChanged
import com.todo.shared.model.UpdateMemberRoleRequest
import com.todo.shared.validation.Validation

class MemberService(
    private val lists: TodoListRepository,
    private val users: UserRepository,
    private val hub: RealtimeHub,
) {
    suspend fun members(userId: String, listId: String): List<MemberDto> {
        requireMember(userId, listId)
        return lists.membersForList(listId)
    }

    suspend fun addMember(userId: String, listId: String, request: AddMemberRequest): MemberDto {
        requireOwner(userId, listId)
        val email = Validation.email(request.email)
        if (!email.valid) throw validationError(email.error!!)
        val role = request.role ?: ListRole.VIEWER
        val roleCheck = Validation.memberRole(role)
        if (!roleCheck.valid) throw validationError(roleCheck.error!!)

        val target = users.findByEmail(request.email)
            ?: throw notFound("No user found with that email.")

        val ownerId = lists.ownerOf(listId) ?: throw notFound("List not found.")
        if (target.id == ownerId) throw conflict("The list owner is already a member.")
        if (lists.isMember(listId, target.id)) {
            throw conflict("This user is already a member of the list.")
        }

        val member = lists.addMember(listId, target, role)
        hub.broadcast(listId, MemberAdded(member, listId))
        return member
    }

    suspend fun removeMember(userId: String, listId: String, targetUserId: String) {
        requireOwner(userId, listId)
        val ownerId = lists.ownerOf(listId) ?: throw notFound("List not found.")
        if (targetUserId == ownerId) {
            throw conflict("The list owner cannot be removed.")
        }
        if (!lists.removeMember(listId, targetUserId)) {
            throw notFound("Member not found.")
        }
        hub.broadcast(listId, MemberRemoved(targetUserId, listId))
    }

    suspend fun updateRole(userId: String, listId: String, targetUserId: String, request: UpdateMemberRoleRequest) {
        requireOwner(userId, listId)
        if (request.role == ListRole.OWNER) {
            throw validationError("The OWNER role cannot be assigned to another user.")
        }
        val ownerId = lists.ownerOf(listId) ?: throw notFound("List not found.")
        if (targetUserId == ownerId) {
            throw conflict("The list owner role cannot be changed.")
        }
        if (!lists.updateRole(listId, targetUserId, request.role)) {
            throw notFound("Member not found.")
        }
        hub.broadcast(listId, MemberRoleChanged(targetUserId, request.role, listId))
    }

    private suspend fun requireMember(userId: String, listId: String) {
        val role = lists.roleOf(userId, listId) ?: throw notFound("List not found.")
        if (role == null) {
            // unreachable, keeps exhaustive semantics
            throw notFound("List not found.")
        }
    }

    private suspend fun requireOwner(userId: String, listId: String) {
        val role = lists.roleOf(userId, listId) ?: throw notFound("List not found.")
        if (role != ListRole.OWNER) {
            throw forbidden("Only the owner can manage members.")
        }
    }
}
