package com.todo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import com.todo.app.state.AppState
import com.todo.shared.model.TodoDto

private val MAX_WIDTH: Dp = 520.dp

@Composable
fun LoginScreen(state: AppState) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Todo", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_WIDTH),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_WIDTH),
            singleLine = true,
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { state.login(email, password) },
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_WIDTH),
            enabled = !state.loading,
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Log in")
            }
        }
        TextButton(onClick = { state.setScreen(com.todo.app.state.Screen.Register) }) {
            Text("Create an account")
        }
    }
}

@Composable
fun RegisterScreen(state: AppState) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_WIDTH),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_WIDTH),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_WIDTH),
            singleLine = true,
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { state.register(name, email, password) },
            modifier = Modifier.fillMaxWidth().widthIn(max = MAX_WIDTH),
            enabled = !state.loading,
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign up")
            }
        }
        TextButton(onClick = { state.setScreen(com.todo.app.state.Screen.Login) }) {
            Text("Already have an account? Log in")
        }
    }
}

@Composable
fun DashboardScreen(state: AppState) {
    var newListName by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("My lists", style = MaterialTheme.typography.headlineMedium)
                state.user?.let {
                    Text(it.email, style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = { state.logout() }) { Text("Log out") }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                label = { Text("New list name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newListName.isNotBlank()) {
                        state.createList(newListName)
                        newListName = ""
                    }
                },
                enabled = !state.loading,
            ) { Text("Add") }
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(state.lists, key = { it.id }) { list ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(list.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${list.members.size} member(s)",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { state.openList(list.id, list.name) }) { Text("Open") }
                        TextButton(onClick = { state.deleteList(list.id) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
fun TodoListScreen(state: AppState, listId: String, listName: String) {
    var newTitle by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(listName, style = MaterialTheme.typography.headlineMedium)
                Text("${state.todosFor(listId).count { it.completed }} / ${state.todosFor(listId).size} done",
                    style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { state.setScreen(com.todo.app.state.Screen.Dashboard) }) {
                Text("Back")
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Add a task") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newTitle.isNotBlank()) {
                        state.addTodo(listId, newTitle)
                        newTitle = ""
                    }
                },
            ) { Text("Add") }
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(state.todosFor(listId), key = { it.id }) { todo ->
                TodoRow(state, listId, todo)
            }
        }
    }
}

@Composable
private fun TodoRow(state: AppState, listId: String, todo: TodoDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = todo.completed,
                onClick = { state.toggleTodo(listId, todo) },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    todo.title,
                    style = if (todo.completed) {
                        MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                )
                if (todo.description.isNotBlank()) {
                    Text(todo.description, style = MaterialTheme.typography.bodySmall)
                }
                todo.createdByName?.let {
                    Text("by $it", style = MaterialTheme.typography.labelSmall)
                }
            }
            TextButton(onClick = { state.deleteTodo(listId, todo.id) }) { Text("Delete") }
        }
    }
}
