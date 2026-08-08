package com.todo.server.config

import java.time.Duration

/**
 * Environment-driven configuration. Never logs or exposes secret values.
 */
data class AppConfig(
    val host: String,
    val port: Int,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val dbMaxPoolSize: Int,
    val jwtSecret: String,
    val jwtIssuer: String,
    val sessionTtl: Duration,
    val cookieSecure: Boolean,
    val corsAllowedOrigins: List<String>,
    val staticRoot: String?,
    val rateLimitMaxPerWindow: Int,
    val rateLimitWindowSeconds: Long,
    val logLevel: String,
) {
    companion object {
        private val booleanValues = setOf("true", "1", "yes", "on")

        private fun env(key: String, default: String): String =
            System.getenv(key)?.takeIf { it.isNotBlank() } ?: default

        private fun envBool(key: String, default: Boolean): Boolean =
            System.getenv(key)?.let { it.lowercase() in booleanValues } ?: default

        fun fromEnv(): AppConfig {
            val jwtSecret = env("JWT_SECRET", "")
            if (jwtSecret.length < 32) {
                error("JWT_SECRET must be set to at least 32 characters in production.")
            }
            return AppConfig(
                host = env("SERVER_HOST", "0.0.0.0"),
                port = env("SERVER_PORT", "8080").toInt(),
                dbUrl = env("DATABASE_URL", "jdbc:postgresql://localhost:5432/todo"),
                dbUser = env("DATABASE_USER", "todo"),
                dbPassword = env("DATABASE_PASSWORD", "todo"),
                dbMaxPoolSize = env("DATABASE_MAX_POOL", "10").toInt(),
                jwtSecret = jwtSecret,
                jwtIssuer = env("JWT_ISSUER", "todo-kmp"),
                sessionTtl = Duration.ofDays(env("SESSION_TTL_DAYS", "7").toLong()),
                cookieSecure = envBool("COOKIE_SECURE", false),
                corsAllowedOrigins = env("CORS_ALLOWED_ORIGINS", "http://localhost:8080")
                    .split(",").map { it.trim() }.filter { it.isNotBlank() },
                staticRoot = System.getenv("STATIC_ROOT")?.takeIf { it.isNotBlank() },
                rateLimitMaxPerWindow = env("RATE_LIMIT_MAX", "20").toInt(),
                rateLimitWindowSeconds = env("RATE_LIMIT_WINDOW_SECONDS", "60").toLong(),
                logLevel = env("LOG_LEVEL", "INFO"),
            )
        }
    }
}
