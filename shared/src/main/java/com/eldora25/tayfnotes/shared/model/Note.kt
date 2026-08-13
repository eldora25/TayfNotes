package com.eldora25.tayfnotes.shared.model

import kotlinx.serialization.Serializable

/**
 * TayfNotes Hybrid Note Model
 */
@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val colorHex: String = "#FFFFFF",
    val emoji: String? = null,
    val type: NoteType = NoteType.TEXT,
    val tags: List<String> = emptyList(),
    val sourceUrl: String? = null,
    val folderId: String? = null,
    val imageUris: List<String> = emptyList(),
    val audioPath: String? = null,
    val sketchData: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val reminderTimestamp: Long? = null,
    val reminderRepeat: RepeatInterval? = null,
    val isLocked: Boolean = false,
    val isArchived: Boolean = false,
    val position: Int = 0,
    val fontFamily: String? = null,
    val fontSize: Float? = null
)

enum class NoteType {
    TEXT, CHECKLIST, SKETCH
}

enum class RepeatInterval {
    NONE, DAILY, WEEKLY, MONTHLY
}

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val colorHex: String = "#757575",
    val noteCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val position: Int = 0 
)

/**
 * Representing a single item in a Checklist
 */
@Serializable
data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
    val subItems: List<ChecklistItem> = emptyList(),
    val position: Int = 0,
    val reminderTimestamp: Long? = null,
    val reminderRepeat: RepeatInterval? = null
)
