package com.todo.server.auth

import at.favre.lib.crypto.bcrypt.BCrypt

/** bcrypt password hashing. Never stores or logs plaintext passwords. */
class PasswordHasher {
    private val bcrypt = BCrypt.withDefaults()
    private val verifyer = BCrypt.verifyer()

    fun hash(plain: String): String = bcrypt.hashToString(COST, plain.toCharArray())

    fun verify(plain: String, hash: String): Boolean =
        verifyer.verify(plain.toCharArray(), hash).verified

    private companion object {
        const val COST = 12
    }
}
