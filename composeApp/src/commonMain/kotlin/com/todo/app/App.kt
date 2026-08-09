package com.todo.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.todo.app.state.AppState
import com.todo.app.state.Screen
import com.todo.app.ui.DashboardScreen
import com.todo.app.ui.LoginScreen
import com.todo.app.ui.RegisterScreen
import com.todo.app.ui.TodoListScreen

@Composable
private fun rememberAppState(): AppState = remember { AppState() }

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val state = rememberAppState()
            when (val screen = state.currentScreen) {
                is Screen.Login -> LoginScreen(state)
                is Screen.Register -> RegisterScreen(state)
                is Screen.Dashboard -> DashboardScreen(state)
                is Screen.TodoList -> TodoListScreen(state, screen.listId, screen.listName)
            }
        }
    }
}
