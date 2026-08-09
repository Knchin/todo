package com.todo.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todo.app.presentation.NotesViewModel
import com.todo.shared.model.NoteSummary
import com.todo.shared.model.NoteType
import kotlin.time.Clock

internal val MaxWidth = 640.dp

@Composable
fun HomeScreen(viewModel: NotesViewModel, onCreateClick: () -> Unit, onOpenNote: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Notes", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Create and share notes instantly.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onCreateClick, modifier = Modifier.width(220.dp)) {
            Text("Create a note")
        }
        Spacer(Modifier.height(24.dp))

        when {
            viewModel.homeLoading -> LoadingBlock("Loading notes...")
            viewModel.homeError != null -> ErrorBlock(viewModel.homeError!!) { viewModel.loadHome() }
            viewModel.notes.isEmpty() -> EmptyState(onCreateClick)
            else -> PublicNotesList(viewModel.notes, onOpenNote)
        }
    }
}

@Composable
internal fun LoadingBlock(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun EmptyState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No public notes yet.", style = MaterialTheme.typography.titleMedium)
        Text("Create the first one.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreateClick) { Text("Create a note") }
    }
}

@Composable
private fun PublicNotesList(notes: List<NoteSummary>, onOpenNote: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Public notes",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().width(MaxWidth),
        )
        Spacer(Modifier.height(12.dp))
        notes.forEach { note ->
            Card(
                modifier = Modifier.fillMaxWidth().width(MaxWidth).padding(vertical = 6.dp)
                    .clickable { onOpenNote(note.publicId) },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(note.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (note.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            note.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Updated ${relativeTime(note.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Open note →", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

internal fun relativeTime(epochMillis: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - epochMillis
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000} minute(s) ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hour(s) ago"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000} day(s) ago"
        else -> "long ago"
    }
}
