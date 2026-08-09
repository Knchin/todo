package com.todo.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.todo.app.data.AppConfig
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class, ExperimentalComposeUiApi::class)
fun main() {
    val config = appConfig()
    val isConfigured = config.supabaseUrl.isNotEmpty()
        && config.supabaseAnonKey.isNotEmpty()
        && !config.supabaseUrl.startsWith("@@")
        && !config.supabaseAnonKey.startsWith("@@")
    if (isConfigured) {
        AppConfig.supabaseUrl = config.supabaseUrl
        AppConfig.supabaseAnonKey = config.supabaseAnonKey
    }
    ComposeViewport {
        App()
    }
}

// `js()` is the Kotlin/Wasm way to call out to a global expression; it must
// appear as the single expression of a top-level function body or property
// initializer. It returns a `dynamic` value pointing at `window.APP_CONFIG`,
// which is the plain JS object set by index.html at build time.
private fun appConfig(): AppConfigJs = js("window.APP_CONFIG")

private external interface AppConfigJs {
    val supabaseUrl: String
    val supabaseAnonKey: String
}
