package com.todo.server.database

import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

data class UserRecord(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Long,
    val updatedAt: Long,
)

private fun ResultRow.toUserRecord() = UserRecord(
    id = this[Users.id].toString(),
    name = this[Users.name],
    email = this[Users.email],
    passwordHash = this[Users.passwordHash],
    createdAt = this[Users.createdAt],
    updatedAt = this[Users.updatedAt],
)

class UserRepository {
    suspend fun findByEmail(email: String): UserRecord? = withDb {
        Users
            .selectAll()
            .where { Users.email eq email.trim().lowercase() }
            .firstOrNull()
            ?.toUserRecord()
    }

    suspend fun findById(id: String): UserRecord? = withDb {
        Users
            .selectAll()
            .where { Users.id eq Uuid.parse(id) }
            .firstOrNull()
            ?.toUserRecord()
    }

    suspend fun create(name: String, email: String, passwordHash: String): UserRecord = withDb {
        val now = System.currentTimeMillis()
        val id = Uuid.parse(java.util.UUID.randomUUID().toString())
        Users.insert {
            it[Users.id] = id
            it[Users.name] = name
            it[Users.email] = email.trim().lowercase()
            it[Users.passwordHash] = passwordHash
            it[Users.createdAt] = now
            it[Users.updatedAt] = now
        }
        Users.selectAll().where { Users.id eq id }.firstOrNull()!!.toUserRecord()
    }
}
