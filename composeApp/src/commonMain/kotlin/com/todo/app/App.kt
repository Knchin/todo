package com.todo.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.todo.app.data.AppConfig
import com.todo.app.presentation.NotesViewModel
import com.todo.app.presentation.Screen
import com.todo.app.ui.CreateNoteScreen
import com.todo.app.ui.HomeScreen
import com.todo.app.ui.NoteScreen
import com.todo.shared.model.NoteType
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val viewModel = remember { NotesViewModel() }
            Router(viewModel)
        }
    }
}

@Composable
private fun Router(viewModel: NotesViewModel) {
    LaunchedEffect(Unit) {
        val path = Platform.currentPath()
        if (path.startsWith("/n/")) {
            val publicId = path.substringAfter("/n/").trim().ifBlank { null }
            if (publicId != null) viewModel.navigate(Screen.ViewNote(publicId))
        }
    }

    when (val screen = viewModel.screen) {
        is Screen.Home -> HomeScreen(
            viewModel = viewModel,
            onCreateClick = {
                viewModel.navigate(Screen.CreateNote(NoteType.PUBLIC))
                Platform.setUrlPath("/create")
            },
            onOpenNote = { publicId -> openNote(viewModel, publicId) },
        )
        is Screen.CreateNote -> CreateNoteScreen(
            viewModel = viewModel,
            initialType = screen.type,
            onBack = {
                viewModel.navigate(Screen.Home)
                Platform.setUrlPath("/")
            },
            onOpenNote = { publicId -> openNote(viewModel, publicId) },
            onCopyLink = { publicId -> copyLink(publicId) },
        )
        is Screen.ViewNote -> NoteScreen(
            viewModel = viewModel,
            publicId = screen.publicId,
            onBack = {
                viewModel.navigate(Screen.Home)
                Platform.setUrlPath("/")
            },
        )
    }
}

private fun openNote(viewModel: NotesViewModel, publicId: String) {
    viewModel.navigate(Screen.ViewNote(publicId))
    Platform.setUrlPath("/n/$publicId")
}

private fun copyLink(publicId: String) {
    Platform.copyText("/n/$publicId")
}
