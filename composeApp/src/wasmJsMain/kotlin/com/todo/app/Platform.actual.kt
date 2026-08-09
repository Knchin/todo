package com.todo.app

import kotlinx.browser.window

actual object Platform {
    actual fun setUrlPath(path: String) {
        window.history.pushState(data = null, title = "", url = path)
    }

    actual fun copyText(text: String) {
        // Best-effort; failures are ignored. navigator.clipboard is available
        // only in secure contexts and may be null.
        val clipboard = window.navigator.clipboard
        if (clipboard != null) {
            clipboard.writeText(text)
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
