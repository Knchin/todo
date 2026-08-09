package com.todo.app

import kotlinx.browser.window

actual object Platform {
    actual fun setUrlPath(path: String) {
        window.history.pushState(null, "", path)
    }

    actual fun copyText(text: String) {
        val clipboard = window.navigator.clipboard
        if (clipboard != null) {
            // Best-effort; failures are ignored.
            val promise = clipboard.writeText(text)
        }
    }

    actual fun currentPath(): String {
        val path = window.location.pathname
        return if (path.startsWith("/n/")) {
            val publicId = path.substringAfter("/n/")
            if (publicId.isBlank()) "/" else "/n/$publicId"
        } else {
            "/"
        }
    }
}
