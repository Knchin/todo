package com.todo.server.http

import com.todo.shared.model.ErrorCodes
import io.ktor.http.HttpStatusCode

/**
 * A typed error that can be converted into a consistent API error response.
 * Sensitive internals must never be placed in [message] for production-facing errors.
 */
class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
) : RuntimeException(message)

fun validationError(message: String): ApiException =
    ApiException(HttpStatusCode.BadRequest, ErrorCodes.VALIDATION, message)

fun unauthorized(message: String = "Authentication is required."): ApiException =
    ApiException(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED, message)

fun invalidCredentials(): ApiException =
    ApiException(HttpStatusCode.Unauthorized, ErrorCodes.INVALID_CREDENTIALS, "Invalid email or password.")

fun forbidden(message: String = "You do not have permission to perform this action."): ApiException =
    ApiException(HttpStatusCode.Forbidden, ErrorCodes.FORBIDDEN, message)

fun notFound(message: String = "The requested resource was not found."): ApiException =
    ApiException(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND, message)

fun conflict(message: String): ApiException =
    ApiException(HttpStatusCode.Conflict, ErrorCodes.CONFLICT, message)

fun rateLimited(message: String = "Too many requests. Please try again later."): ApiException =
    ApiException(HttpStatusCode.TooManyRequests, ErrorCodes.RATE_LIMITED, message)
