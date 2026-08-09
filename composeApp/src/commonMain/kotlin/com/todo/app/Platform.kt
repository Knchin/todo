package com.todo.app

/** Platform-specific helpers (URL routing, clipboard). */
expect object Platform {
    /** Navigate to a client-side path without a full page reload. */
    fun setUrlPath(path: String)

    /** Copy text to the clipboard (best effort). */
    fun copyText(text: String)

    /** Current URL path, e.g. "/n/abc123". */
    fun currentPath(): String
}
