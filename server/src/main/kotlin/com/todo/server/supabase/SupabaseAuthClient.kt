package com.todo.server.supabase

import com.todo.server.http.ApiException
import com.todo.server.http.invalidCredentials
import com.todo.server.http.conflict
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Minimal client for the Supabase Auth (GoTrue) REST API. Used instead of the
 * self-hosted bcrypt + HMAC-JWT auth: Supabase owns credential hashing and
 * token issuance; this backend only mirrors users into its own tables so list
 * membership keeps working.
 */
class SupabaseAuthClient(
    private val baseUrl: String,
    private val publishableKey: String,
    private val json: Json,
    private val http: HttpClient,
) {
    private val signupUrl = "$baseUrl/auth/v1/signup"
    private val tokenUrl = "$baseUrl/auth/v1/token?grant_type=password"
    private val logoutUrl = "$baseUrl/auth/v1/logout"
    private val userUrl = "$baseUrl/auth/v1/user"

    suspend fun signUp(email: String, password: String, name: String): SupabaseUser {
        val response = http.post(signupUrl) {
            contentType(ContentType.Application.Json)
            header("apikey", publishableKey)
            header("Authorization", "Bearer $publishableKey")
            setBody(
                json.encodeToString(
                    SignUpRequest(email = email, password = password, options = SignUpOptions(data = UserData(name))),
                ),
            )
        }
        if (response.status == HttpStatusCode.Conflict) {
            throw conflict("An account with this email already exists.")
        }
        if (!response.status.isSuccess()) {
            throw fromError(response)
        }
        return response.body<SupabaseUser>()
    }

    suspend fun signIn(email: String, password: String): SupabaseSession {
        val response = http.post(tokenUrl) {
            contentType(ContentType.Application.Json)
            header("apikey", publishableKey)
            header("Authorization", "Bearer $publishableKey")
            setBody(json.encodeToString(PasswordGrant(email = email, password = password)))
        }
        if (response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.Unauthorized) {
            throw invalidCredentials()
        }
        if (!response.status.isSuccess()) {
            throw fromError(response)
        }
        return response.body<SupabaseSession>()
    }

    suspend fun signOut(accessToken: String) {
        runCatching {
            http.post(logoutUrl) {
                header("apikey", publishableKey)
                header("Authorization", "Bearer $accessToken")
            }
        }
    }

    suspend fun getUser(accessToken: String): SupabaseUser? = runCatching {
        http.post(userUrl) {
            header("apikey", publishableKey)
            header("Authorization", "Bearer $accessToken")
        }.takeIf { it.status.isSuccess() }?.body<SupabaseUser>()
    }.getOrNull()

    private suspend fun fromError(response: HttpResponse): ApiException {
        val message = runCatching { response.body<GoTrueError>().errorDescription }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Authentication failed."
        return ApiException(response.status, "SUPABASE_AUTH", message)
    }
}

@Serializable
data class SignUpRequest(val email: String, val password: String, val options: SignUpOptions)

@Serializable
data class SignUpOptions(val data: UserData)

@Serializable
data class UserData(val name: String)

@Serializable
data class PasswordGrant(val email: String, val password: String)

@Serializable
data class SupabaseSession(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: SupabaseUser? = null,
)

@Serializable
data class SupabaseUser(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: SupabaseUserMetadata = SupabaseUserMetadata(),
)

@Serializable
data class SupabaseUserMetadata(val name: String? = null)

@Serializable
data class GoTrueError(
    val code: String = "",
    val msg: String = "",
    @SerialName("error_description") val errorDescription: String = "",
)
