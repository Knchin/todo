package com.todo.server.database

import com.todo.shared.model.ListRole
import com.todo.shared.model.MemberDto
import com.todo.shared.model.TodoListDto
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

private fun ResultRow.toListDto(role: ListRole) = TodoListDto(
    id = this[TodoLists.id].toString(),
    name = this[TodoLists.name],
    ownerId = this[TodoLists.ownerId].toString(),
    createdAt = this[TodoLists.createdAt],
    updatedAt = this[TodoLists.updatedAt],
    role = role,
    members = emptyList(),
)

private fun ResultRow.toMemberDto(role: ListRole) = MemberDto(
    userId = this[ListMembers.userId].toString(),
    name = this[Users.name],
    email = this[Users.email],
    role = role,
    joinedAt = this[ListMembers.joinedAt],
)

private fun parseRole(raw: String): ListRole = ListRole.valueOf(raw)

class TodoListRepository {
    /** Lists the given user is a member of, with their role and members. */
    suspend fun listsForUser(userId: String): List<TodoListDto> = withDb {
        val uid = Uuid.parse(userId)
        val rows = (ListMembers innerJoin TodoLists)
            .selectAll()
            .where { ListMembers.userId eq uid }
            .orderBy(TodoLists.createdAt to SortOrder.DESC)
            .toList()

        val listIds = rows.map { it[ListMembers.listId] }
        val membersById = membersForListIds(listIds)

        rows.map { row ->
            val listId = row[ListMembers.listId]
            row.toListDto(parseRole(row[ListMembers.role]))
                .copy(members = membersById[listId].orEmpty())
        }
    }

    /** Role of the user in the list, or null if the user is not a member. */
    suspend fun roleOf(userId: String, listId: String): ListRole? = withDb {
        ListMembers
            .selectAll()
            .where { (ListMembers.listId eq Uuid.parse(listId)) and (ListMembers.userId eq Uuid.parse(userId)) }
            .firstOrNull()
            ?.let { parseRole(it[ListMembers.role]) }
    }

    /** The list plus the requesting user's role; null if list missing or user not a member. */
    suspend fun findForUser(userId: String, listId: String): TodoListDto? = withDb {
        val role = roleOf(userId, listId) ?: return@withDb null
        TodoLists
            .selectAll()
            .where { TodoLists.id eq Uuid.parse(listId) }
            .firstOrNull()
            ?.toListDto(role)
            ?.copy(members = membersForList(listId))
    }

    suspend fun ownerOf(listId: String): String? = withDb {
        TodoLists
            .selectAll()
            .where { TodoLists.id eq Uuid.parse(listId) }
            .firstOrNull()
            ?.let { it[TodoLists.ownerId].toString() }
    }

    suspend fun create(ownerId: String, name: String): TodoListDto = withDb {
        val now = System.currentTimeMillis()
        val listId = Uuid.parse(java.util.UUID.randomUUID().toString())
        val ownerUuid = Uuid.parse(ownerId)

        TodoLists.insert {
            it[TodoLists.id] = listId
            it[TodoLists.name] = name
            it[TodoLists.ownerId] = ownerUuid
            it[TodoLists.createdAt] = now
            it[TodoLists.updatedAt] = now
        }
        ListMembers.insert {
            it[ListMembers.listId] = listId
            it[ListMembers.userId] = ownerUuid
            it[ListMembers.role] = ListRole.OWNER.name
            it[ListMembers.joinedAt] = now
        }

        TodoLists.selectAll().where { TodoLists.id eq listId }
            .firstOrNull()!!
            .toListDto(ListRole.OWNER)
            .copy(members = membersForList(listId.toString()))
    }

    /** Returns true when exactly one row was renamed. */
    suspend fun rename(listId: String, name: String): Boolean = withDb {
        TodoLists.update({ TodoLists.id eq Uuid.parse(listId) }) {
            it[TodoLists.name] = name
            it[TodoLists.updatedAt] = System.currentTimeMillis()
        } == 1
    }

    suspend fun delete(listId: String): Boolean = withDb {
        TodoLists.deleteWhere { TodoLists.id eq Uuid.parse(listId) } > 0
    }

    suspend fun membersForList(listId: String): List<MemberDto> = withDb {
        val listUuid = Uuid.parse(listId)
        (ListMembers innerJoin Users)
            .selectAll()
            .where { ListMembers.listId eq listUuid }
            .orderBy(ListMembers.joinedAt to SortOrder.ASC)
            .toList()
            .map { it.toMemberDto(parseRole(it[ListMembers.role])) }
    }

    private fun membersForListIds(listIds: List<Uuid>): Map<Uuid, List<MemberDto>> {
        if (listIds.isEmpty()) return emptyMap()
        val rows = (ListMembers innerJoin Users)
            .selectAll()
            .where { ListMembers.listId inList listIds }
            .toList()
        return rows.groupBy({ it[ListMembers.listId] }, { it.toMemberDto(parseRole(it[ListMembers.role])) })
    }

    suspend fun addMember(listId: String, user: UserRecord, role: ListRole): MemberDto = withDb {
        val listUuid = Uuid.parse(listId)
        val now = System.currentTimeMillis()
        ListMembers.insert {
            it[ListMembers.listId] = listUuid
            it[ListMembers.userId] = Uuid.parse(user.id)
            it[ListMembers.role] = role.name
            it[ListMembers.joinedAt] = now
        }
        MemberDto(
            userId = user.id,
            name = user.name,
            email = user.email,
            role = role,
            joinedAt = now,
        )
    }

    suspend fun isMember(listId: String, userId: String): Boolean = withDb {
        ListMembers
            .selectAll()
            .where {
                (ListMembers.listId eq Uuid.parse(listId)) and (ListMembers.userId eq Uuid.parse(userId))
            }
            .any()
    }

    suspend fun removeMember(listId: String, userId: String): Boolean = withDb {
        ListMembers.deleteWhere {
            (ListMembers.listId eq Uuid.parse(listId)) and (ListMembers.userId eq Uuid.parse(userId))
        } > 0
    }

    suspend fun updateRole(listId: String, userId: String, role: ListRole): Boolean = withDb {
        ListMembers.update({
            (ListMembers.listId eq Uuid.parse(listId)) and (ListMembers.userId eq Uuid.parse(userId))
        }) {
            it[ListMembers.role] = role.name
        } > 0
    }
}
