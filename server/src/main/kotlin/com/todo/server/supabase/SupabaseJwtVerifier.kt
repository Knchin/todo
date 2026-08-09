package com.todo.server.supabase

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Validates Supabase-issued JWTs (RS256) against the project's JWKS endpoint.
 * This replaces the self-hosted HMAC [com.todo.server.auth.JwtService] whenever
 * Supabase Auth is enabled: access tokens minted by GoTrue authenticate API
 * requests and the realtime socket.
 */
class SupabaseJwtVerifier(
    private val baseUrl: String,
    private val http: HttpClient,
    private val json: Json,
) {
    private val logger = LoggerFactory.getLogger(SupabaseJwtVerifier::class.java)
    private val jwksUrl = "$baseUrl/auth/v1/.well-known/jwks.json"
    private val issuer = "$baseUrl/auth/v1"

    private val publicKeys = ConcurrentHashMap<String, RSAPublicKey>()
    @Volatile private var keysLoaded = false

    private val provider = object : RSAKeyProvider {
        override fun getPublicKeyById(keyId: String?): RSAPublicKey {
            refreshKeysIfNeeded()
            return publicKeys[keyId]
                ?: throw IllegalStateException("No public key for kid $keyId")
        }

        override fun getPrivateKey(): RSAPrivateKey? = null
        override fun getPrivateKeyId(): String? = null
    }

    private val verifier = JWT.require(Algorithm.RSA256(provider))
        .withIssuer(issuer)
        .build()

    fun verifyToken(token: String): String? = try {
        val jwt = verifier.verify(token)
        jwt.subject?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        logger.debug("Supabase token verification failed: {}", e.message)
        null
    }

    private fun refreshKeysIfNeeded() {
        if (keysLoaded) return
        synchronized(this) {
            if (keysLoaded) return
            runCatching {
                val raw = runBlocking { http.get(jwksUrl).bodyAsText() }
                val jwks = json.decodeFromString<Jwks>(raw)
                for (jwk in jwks.keys) {
                    val publicKey = buildPublicKey(jwk) ?: continue
                    publicKeys[jwk.kid] = publicKey
                }
                keysLoaded = true
            }.onFailure { logger.warn("Failed to load Supabase JWKS: {}", it.message) }
        }
    }

    private fun buildPublicKey(jwk: Jwk): RSAPublicKey? {
        val modulus = jwk.n?.let { decode(it) } ?: return null
        val exponent = jwk.e?.let { decode(it) } ?: return null
        return KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(BigInteger(1, modulus), BigInteger(1, exponent)))
            as RSAPublicKey
    }

    private fun decode(base64Url: String): ByteArray =
        Base64.getUrlDecoder().decode(base64Url)
}

@Serializable
data class Jwks(val keys: List<Jwk> = emptyList())

@Serializable
data class Jwk(
    val kid: String = "",
    val kty: String = "",
    val alg: String = "",
    val n: String? = null,
    val e: String? = null,
)
