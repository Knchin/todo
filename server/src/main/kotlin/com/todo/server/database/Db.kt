package com.todo.server.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/** Holds the active [Database] so repositories and tests can share one instance. */
object DatabaseContext {
    lateinit var current: Database
        private set

    fun set(database: Database) {
        current = database
    }
}

/**
 * Runs an Exposed transaction on an IO dispatcher, safe to call from Ktor
 * suspend handlers without blocking the event loop.
 */
suspend fun <T> withDb(block: suspend JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction(DatabaseContext.current, null, null) { block() }
    }
