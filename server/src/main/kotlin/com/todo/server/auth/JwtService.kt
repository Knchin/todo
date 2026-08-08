package com.todo.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.todo.server.config.AppConfig
import java.util.Date

class JwtService(config: AppConfig) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)
    private val issuer = config.jwtIssuer
    private val ttlMillis = config.sessionTtl.toMillis()
    private val verifier = JWT.require(algorithm).withIssuer(issuer).build()

    fun issueToken(userId: String): String {
        val now = Date()
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(userId)
            .withIssuedAt(now)
            .withExpiresAt(Date(now.time + ttlMillis))
            .sign(algorithm)
    }

    /** Returns the user id if the token is valid and unexpired, otherwise null. */
    fun verifyToken(token: String): String? = try {
        verifier.verify(token).subject
    } catch (_: Exception) {
        null
    }
}
