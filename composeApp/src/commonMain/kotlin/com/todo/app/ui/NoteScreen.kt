package com.todo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.todo.app.presentation.NotesViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun NoteScreen(
    viewModel: NotesViewModel,
    publicId: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().width(MaxWidth), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
        }

        when {
            viewModel.noteLoading -> LoadingBlock("Loading note...")
            viewModel.noteError != null -> ErrorBlock(viewModel.noteError!!) { viewModel.loadNote(publicId) }
            viewModel.noteLocked -> UnlockBlock(viewModel, publicId)
            viewModel.note != null -> NoteContent(viewModel, onBack)
            else -> ErrorBlock("Note not found.", onBack)
        }
    }
}

@Composable
private fun UnlockBlock(viewModel: NotesViewModel, publicId: String) {
    var passcode by remember { mutableStateOf("") }
    Card(Modifier.fillMaxWidth().width(MaxWidth)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Protected note", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "This note requires a passcode.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it },
                label = { Text("Passcode") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            viewModel.passcodeError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.unlock(publicId, passcode) }, modifier = Modifier.fillMaxWidth()) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun NoteContent(viewModel: NotesViewModel, onBack: () -> Unit) {
    val note = viewModel.note ?: return
    var editing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            onCancel = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteNote(note.publicId) { onBack() }
            },
        )
    }

    if (editing) {
        EditNoteForm(
            viewModel = viewModel,
            publicId = note.publicId,
            initialName = note.name,
            initialDescription = note.description,
            initialContent = note.content,
            onCancel = { editing = false },
            onSaved = { editing = false },
        )
        return
    }

    Column(Modifier.fillMaxWidth().width(MaxWidth)) {
        Text(note.name, style = MaterialTheme.typography.headlineMedium)
        if (note.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(note.description, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))
        Text(note.content, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        Text(
            "Created: ${formatDate(note.createdAt)}  ·  Updated: ${formatDate(note.updatedAt)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        viewModel.saveError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Row {
            Button(onClick = { editing = true }) { Text("Edit") }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = { showDeleteConfirm = true }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EditNoteForm(
    viewModel: NotesViewModel,
    publicId: String,
    initialName: String,
    initialDescription: String,
    initialContent: String,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var content by remember { mutableStateOf(initialContent) }

    Column(Modifier.fillMaxWidth().width(MaxWidth)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            minLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        viewModel.saveError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Button(
                onClick = { viewModel.updateNote(publicId, name, description, content) { onSaved() } },
                enabled = !viewModel.saving,
            ) {
                if (viewModel.saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onCancel, enabled = !viewModel.saving) { Text("Cancel") }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete note?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

internal fun formatDate(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.year}-${pad(local.monthNumber)}-${pad(local.dayOfMonth)} ${pad(local.hour)}:${pad(local.minute)}"
}

private fun pad(value: Int): String = if (value < 10) "0$value" else value.toString()
