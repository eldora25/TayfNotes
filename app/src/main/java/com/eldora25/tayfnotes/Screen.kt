package com.eldora25.tayfnotes

import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType

sealed class Screen {
    object Main : Screen()
    object Folders : Screen()
    object Calendar : Screen()
    object Archive : Screen()
    object Trash : Screen()
    object Settings : Screen()
    object ThemeSelection : Screen()
    object Info : Screen()
    object InternalBrowser : Screen()
    data class EditNote(val note: Note? = null, val initialSketch: Boolean = false) : Screen()
    data class WebClipper(val url: String, val title: String? = null, val content: String? = null) : Screen()
    data class Placeholder(val title: String) : Screen()
    data class DetailNote(val note: Note) : Screen()
    data class List(val type: NoteType?) : Screen()
}
