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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.todo.app.presentation.NotesViewModel
import com.todo.shared.model.NoteType

@Composable
fun CreateNoteScreen(
    viewModel: NotesViewModel,
    initialType: NoteType,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onCopyLink: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().width(MaxWidth), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("← Back", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text("Create a note", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        viewModel.createdNote?.let { holder ->
            SuccessCard(holder.publicId, onOpenNote, onCopyLink)
            return@Column
        }

        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var passcode by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(initialType) }

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
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("Note visibility", style = MaterialTheme.typography.titleSmall)
            TypeOption("Public", "Visible to everyone on the homepage", NoteType.PUBLIC, selectedType) {
                selectedType = it
            }
            TypeOption("Protected", "Requires a passcode", NoteType.PROTECTED, selectedType) {
                selectedType = it
            }

            if (selectedType == NoteType.PROTECTED) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            viewModel.createError?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.create(selectedType, name, description, content, passcode) },
                enabled = !viewModel.creating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (viewModel.creating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Text("Create note")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TypeOption(
    title: String,
    subtitle: String,
    type: NoteType,
    selected: NoteType,
    onSelect: (NoteType) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected == type, onClick = { onSelect(type) })
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SuccessCard(publicId: String, onOpenNote: (String) -> Unit, onCopyLink: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().width(MaxWidth)) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Your note is ready!", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "/n/$publicId",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onOpenNote(publicId) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open note")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onCopyLink(publicId) }, modifier = Modifier.fillMaxWidth()) {
                Text("Copy link")
            }
        }
    }
}
