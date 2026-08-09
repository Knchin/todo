package com.todo.app.data

/**
 * Runtime configuration for the web app, injected via a global JS object
 * (see wasmJsMain/resources/index.html). These are public, browser-safe
 * values (Supabase URL + anon/publishable key). No service-role secrets are
 * ever exposed to the browser.
 */
object AppConfig {
    var supabaseUrl: String = ""
    var supabaseAnonKey: String = ""

    fun isConfigured(): Boolean = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}
