package com.todo.server.routes

import com.todo.server.config.AppConfig
import com.todo.server.http.notFound
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.io.File

/**
 * Serves the compiled Compose WebAssembly frontend (SPA). Registered last so
 * `/api` and `/ws` routes always win. Paths that do not map to a file fall
 * back to `index.html`, which lets the frontend own its client-side routing.
 */
fun Route.staticContent(config: AppConfig) {
    val root = config.staticRoot?.let(::File)
        ?: listOf(
            File("composeApp/build/dist/wasmJs/productionExecutable"),
            File("../composeApp/build/dist/wasmJs/productionExecutable"),
        ).firstOrNull { it.isDirectory }
    if (root == null || !root.isDirectory) return

    val index = File(root, "index.html")

    route("/") {
        get { call.respondFile(index) }
    }
    route("/{path...}") {
        get {
            val path = call.parameters["path"] ?: ""
            // Never let the SPA fallback swallow API or realtime traffic.
            if (path.startsWith("api/") || path == "ws") throw notFound()
            val file = File(root, path).takeIf { it.isFile } ?: index
            call.respondFile(file)
        }
    }
}
