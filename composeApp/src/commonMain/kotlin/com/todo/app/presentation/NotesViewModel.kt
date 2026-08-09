package com.todo.app.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.todo.app.data.AppConfig
import com.todo.app.data.NoteResult
import com.todo.app.data.NotesApi
import com.todo.shared.model.CreateNoteRequest
import com.todo.shared.model.Note
import com.todo.shared.model.NoteSummary
import com.todo.shared.model.NoteType
import com.todo.shared.validation.NoteValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class ViewNote(val publicId: String) : Screen()
    data class CreateNote(val type: NoteType) : Screen()
}

/** Central presentation state for the anonymous notes app. */
class NotesViewModel(private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    // Home
    var notes by mutableStateOf<List<NoteSummary>>(emptyList())
        private set
    var homeLoading by mutableStateOf(false)
        private set
    var homeError by mutableStateOf<String?>(null)
        private set

    // Note page
    var note by mutableStateOf<Note?>(null)
        private set
    var noteLocked by mutableStateOf(false)
        private set
    var noteLoading by mutableStateOf(false)
        private set
    var noteError by mutableStateOf<String?>(null)
        private set
    var unlocked by mutableStateOf(false)
        private set
    var unlockToken by mutableStateOf<String?>(null)
        private set
    var passcodeError by mutableStateOf<String?>(null)
        private set

    // Create
    var creating by mutableStateOf(false)
        private set
    var createError by mutableStateOf<String?>(null)
        private set
    var createdNote by mutableStateOf<CreateNoteResponseHolder?>(null)
        private set

    // Mutations
    var saving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set
    var deleted by mutableStateOf(false)
        private set

    fun init() {
        loadHome()
    }

    fun navigate(screen: Screen) {
        this.screen = screen
        when (screen) {
            is Screen.Home -> loadHome()
            is Screen.ViewNote -> loadNote(screen.publicId)
            is Screen.CreateNote -> resetCreate()
        }
    }

    // --- Home ---

    fun loadHome() {
        if (!AppConfig.isConfigured()) {
            homeError = "Application is not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY."
            return
        }
        homeLoading = true
        homeError = null
        scope.launch {
            when (val r = NotesApi.listPublicNotes()) {
                is NoteResult.Success -> {
                    notes = r.value
                    homeLoading = false
                }
                is NoteResult.Failure -> {
                    homeError = r.message
                    homeLoading = false
                }
            }
        }
    }

    // --- Note viewing ---

    fun loadNote(publicId: String) {
        noteLoading = true
        noteError = null
        note = null
        noteLocked = false
        unlocked = false
        unlockToken = null
        scope.launch {
            when (val r = NotesApi.getNote(publicId)) {
                is NoteResult.Success -> {
                    noteLoading = false
                    val payload = r.value
                    note = payload.note
                    noteLocked = payload.locked
                }
                is NoteResult.Failure -> {
                    noteLoading = false
                    noteError = r.message
                }
            }
        }
    }

    fun unlock(publicId: String, passcode: String) {
        passcodeError = null
        scope.launch {
            when (val r = NotesApi.unlockNote(publicId, passcode)) {
                is NoteResult.Success -> {
                    note = r.value.note
                    unlockToken = r.value.token
                    unlocked = true
                    noteLocked = false
                }
                is NoteResult.Failure -> passcodeError = r.message
            }
        }
    }

    // --- Create ---

    fun create(type: NoteType, name: String, description: String, content: String, passcode: String) {
        val validation = NoteValidator.create(name, description, content, type, passcode.takeIf { type == NoteType.PROTECTED })
        if (!validation.valid) {
            createError = validation.error
            return
        }
        creating = true
        createError = null
        scope.launch {
            val request = CreateNoteRequest(
                name = name.trim(),
                description = description.trim(),
                content = content.trim(),
                type = type,
                passcode = passcode.takeIf { it.isNotBlank() },
            )
            when (val r = NotesApi.createNote(request)) {
                is NoteResult.Success -> createdNote = CreateNoteResponseHolder(r.value.publicUrl)
                is NoteResult.Failure -> createError = r.message
            }
            creating = false
        }
    }

    fun resetCreate() {
        creating = false
        createError = null
        createdNote = null
    }

    // --- Mutations ---

    fun updateNote(
        publicId: String,
        name: String,
        description: String,
        content: String,
        onDone: () -> Unit,
    ) {
        saving = true
        saveError = null
        scope.launch {
            when (val r = NotesApi.updateNote(
                com.todo.shared.model.UpdateNoteRequest(publicId, name.trim(), description.trim(), content.trim()),
                unlockToken,
            )) {
                is NoteResult.Success -> {
                    note = r.value
                    saving = false
                    onDone()
                }
                is NoteResult.Failure -> {
                    saveError = r.message
                    saving = false
                }
            }
        }
    }

    fun deleteNote(publicId: String, onDone: () -> Unit) {
        saving = true
        saveError = null
        scope.launch {
            when (val r = NotesApi.deleteNote(publicId, unlockToken)) {
                is NoteResult.Success -> {
                    deleted = true
                    saving = false
                    onDone()
                }
                is NoteResult.Failure -> {
                    saveError = r.message
                    saving = false
                }
            }
        }
    }
}

data class CreateNoteResponseHolder(val publicId: String)
