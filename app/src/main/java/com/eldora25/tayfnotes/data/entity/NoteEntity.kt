package com.eldora25.tayfnotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.shared.model.RepeatInterval

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val colorHex: String,
    val emoji: String? = null,
    val type: String,
    val tags: String,
    val sourceUrl: String? = null,
    val folderId: String?,
    val imageUris: String,
    val audioPath: String?,
    val sketchData: String?,
    val createdAt: Long,
    val lastModified: Long,
    val reminderTimestamp: Long?,
    val reminderRepeat: String? = null,
    val isLocked: Boolean,
    val isArchived: Boolean = false,
    val position: Int = 0,
    val fontFamily: String? = null,
    val fontSize: Float? = null,
    val fontColorHex: String? = null
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        colorHex = colorHex,
        emoji = emoji,
        type = NoteType.valueOf(type),
        tags = if (tags.isEmpty()) emptyList() else tags.split(","),
        sourceUrl = sourceUrl,
        folderId = folderId,
        imageUris = if (imageUris.isEmpty()) emptyList() else imageUris.split(","),
        audioPath = audioPath,
        sketchData = sketchData,
        createdAt = createdAt,
        lastModified = lastModified,
        reminderTimestamp = reminderTimestamp,
        reminderRepeat = reminderRepeat?.let { RepeatInterval.valueOf(it) },
        isLocked = isLocked,
        isArchived = isArchived,
        position = position,
        fontFamily = fontFamily,
        fontSize = fontSize,
        fontColorHex = fontColorHex
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            colorHex = note.colorHex,
            emoji = note.emoji,
            type = note.type.name,
            tags = note.tags.joinToString(","),
            sourceUrl = note.sourceUrl,
            folderId = note.folderId,
            imageUris = note.imageUris.joinToString(","),
            audioPath = note.audioPath,
            sketchData = note.sketchData,
            createdAt = note.createdAt,
            lastModified = note.lastModified,
            reminderTimestamp = note.reminderTimestamp,
            reminderRepeat = note.reminderRepeat?.name,
            isLocked = note.isLocked,
            isArchived = note.isArchived,
            position = note.position,
            fontFamily = note.fontFamily,
            fontSize = note.fontSize,
            fontColorHex = note.fontColorHex
        )
    }
}
