package com.todo.app

actual object Platform {
    actual fun setUrlPath(path: String) = Unit
    actual fun copyText(text: String) = Unit
    actual fun currentPath(): String = "/"
}
