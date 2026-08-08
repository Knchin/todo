package com.todo.server.routes

import com.todo.server.auth.SessionCookie
import com.todo.server.auth.requireUserId
import com.todo.server.http.forbidden
import com.todo.server.http.validationError
import com.todo.server.services.ListService
import com.todo.server.services.MemberService
import com.todo.server.services.TodoService
import com.todo.shared.model.AddMemberRequest
import com.todo.shared.model.CreateListRequest
import com.todo.shared.model.CreateTodoRequest
import com.todo.shared.model.ReorderTodosRequest
import com.todo.shared.model.UpdateListRequest
import com.todo.shared.model.UpdateMemberRoleRequest
import com.todo.shared.model.UpdateTodoRequest
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.request.httpMethod
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.intercept
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * All authenticated API routes. Requires the session cookie (authentication)
 * and, for mutating requests, the CSRF header. A cross-site form cannot set
 * that header, and SameSite=Lax already blocks cross-site cookie submission.
 */
fun Route.authenticatedApiRoutes(
    listService: ListService,
    todoService: TodoService,
    memberService: MemberService,
) {
    authenticate(SessionCookie.AUTH_PROVIDER) {
        withCsrfProtection()

        route("/api/lists") {
            get {
                call.respond(listService.myLists(call.requireUserId()))
            }
            post {
                val request = call.receive<CreateListRequest>()
                call.respond(HttpStatusCode.Created, listService.create(call.requireUserId(), request))
            }

            route("/{listId}") {
                get {
                    call.respond(listService.get(call.requireUserId(), call.param("listId")))
                }
                patch {
                    val request = call.receive<UpdateListRequest>()
                    call.respond(listService.rename(call.requireUserId(), call.param("listId"), request))
                }
                delete {
                    listService.delete(call.requireUserId(), call.param("listId"))
                    call.respond(HttpStatusCode.NoContent)
                }

                route("/todos") {
                    get {
                        call.respond(todoService.listForList(call.requireUserId(), call.param("listId")))
                    }
                    post {
                        val request = call.receive<CreateTodoRequest>()
                        call.respond(
                            HttpStatusCode.Created,
                            todoService.create(call.requireUserId(), call.param("listId"), request),
                        )
                    }
                    put("/order") {
                        val request = call.receive<ReorderTodosRequest>()
                        todoService.reorder(call.requireUserId(), call.param("listId"), request)
                        call.respond(HttpStatusCode.NoContent)
                    }
                }

                route("/members") {
                    get {
                        call.respond(memberService.members(call.requireUserId(), call.param("listId")))
                    }
                    post {
                        val request = call.receive<AddMemberRequest>()
                        call.respond(
                            HttpStatusCode.Created,
                            memberService.addMember(call.requireUserId(), call.param("listId"), request),
                        )
                    }

                    route("/{memberId}") {
                        patch {
                            val request = call.receive<UpdateMemberRoleRequest>()
                            memberService.updateRole(
                                call.requireUserId(),
                                call.param("listId"),
                                call.param("memberId"),
                                request,
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                        delete {
                            memberService.removeMember(
                                call.requireUserId(),
                                call.param("listId"),
                                call.param("memberId"),
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }

        route("/api/todos/{todoId}") {
            patch {
                val request = call.receive<UpdateTodoRequest>()
                call.respond(todoService.update(call.requireUserId(), call.param("todoId"), request))
            }
            delete {
                todoService.delete(call.requireUserId(), call.param("todoId"))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Route.withCsrfProtection() {
    intercept(ApplicationCallPipeline.Plugins) {
        val method = call.request.httpMethod
        if (method in CSRF_PROTECTED_METHODS &&
            call.request.headers[SessionCookie.CSRF_HEADER] != SessionCookie.CSRF_VALUE
        ) {
            throw forbidden("CSRF protection: required header is missing.")
        }
    }
}

private val CSRF_PROTECTED_METHODS = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch, HttpMethod.Delete)

private fun ApplicationCall.param(name: String): String =
    parameters[name] ?: throw validationError("Missing path parameter '$name'.")
