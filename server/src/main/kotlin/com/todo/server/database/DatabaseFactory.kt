package com.todo.server.database

import com.todo.server.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun createDataSource(config: AppConfig): DataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = withSsl(config)
            username = config.dbUser
            password = config.dbPassword
            maximumPoolSize = config.dbMaxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    /** Appends `sslmode=require` for Supabase-hosted Postgres when enabled. */
    private fun withSsl(config: AppConfig): String {
        if (!config.dbSsl) return config.dbUrl
        val base = config.dbUrl.trimEnd('&')
        val separator = if ('?' in base) '&' else '?'
        val sslMode = if (config.dbUrl.contains("sslmode=")) "" else "$separator" + "sslmode=require"
        return base + sslMode
    }

    fun migrate(dataSource: DataSource): Unit {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    fun init(config: AppConfig): DataSource {
        val dataSource = createDataSource(config)
        migrate(dataSource)
        DatabaseContext.set(Database.connect(dataSource))
        logger.info("Database connected and migrated.")
        return dataSource
    }
}
