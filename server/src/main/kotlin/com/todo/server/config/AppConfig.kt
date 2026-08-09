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
    val supabaseUrl: String = "",
    val supabasePublishableKey: String = "",
    val supabaseSecretKey: String = "",
    val dbSsl: Boolean = false,
) {
    val supabaseEnabled: Boolean get() = supabaseUrl.isNotBlank() && supabasePublishableKey.isNotBlank()

    companion object {
        private val booleanValues = setOf("true", "1", "yes", "on")

        private fun env(key: String, default: String): String =
            System.getenv(key)?.takeIf { it.isNotBlank() } ?: default

        private fun envBool(key: String, default: Boolean): Boolean =
            System.getenv(key)?.let { it.lowercase() in booleanValues } ?: default

        fun fromEnv(): AppConfig {
            val supabaseUrl = env("SUPABASE_URL", "").trimEnd('/')
            val apiKey = env("SUPABASE_API_KEY", "")
            val publishableKey = env("SUPABASE_PUBLISHABLE_KEY", apiKey)
            val jwtSecret = env("JWT_SECRET", "")
            val supabaseEnabled = supabaseUrl.isNotBlank() && publishableKey.isNotBlank()
            // JWT_SECRET is only used by the self-hosted (non-Supabase) auth path.
            if (!supabaseEnabled && jwtSecret.length < 32) {
                error("JWT_SECRET must be set to at least 32 characters in production.")
            }
            return AppConfig(
                host = env("SERVER_HOST", "0.0.0.0"),
                port = env("SERVER_PORT", "8080").toInt(),
                dbUrl = env("DATABASE_URL", supabaseDbUrl(supabaseUrl)),
                dbUser = env("DATABASE_USER", "postgres"),
                dbPassword = env("DATABASE_PASSWORD", env("SUPABASE_DB_PASSWORD", "todo")),
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
                supabaseUrl = supabaseUrl,
                supabasePublishableKey = publishableKey,
                supabaseSecretKey = env("SUPABASE_SECRET_KEY", apiKey),
                dbSsl = envBool("DATABASE_SSL", false),
            )
        }

        /**
         * Builds the JDBC URL for the Supabase Postgres host from the project
         * URL, e.g. `https://<ref>.supabase.co` -> `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres`.
         * Falls back to a local URL when no Supabase URL is configured.
         */
        private fun supabaseDbUrl(supabaseUrl: String): String {
            val ref = Regex("^https?://([^.]+)\\.supabase\\.co/?$")
                .find(supabaseUrl)
                ?.groupValues
                ?.get(1)
            return if (ref != null) {
                "jdbc:postgresql://db.$ref.supabase.co:5432/postgres"
            } else {
                "jdbc:postgresql://localhost:5432/todo"
            }
        }
    }
}
