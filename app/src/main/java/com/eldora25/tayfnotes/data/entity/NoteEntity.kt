package com.eldora25.tayfnotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val colorHex: String,
    val type: String,
    val tags: String,
    val folderId: String?,
    val imageUris: String,
    val audioPath: String?,
    val sketchData: String?,
    val createdAt: Long,
    val lastModified: Long,
    val reminderTimestamp: Long?,
    val isLocked: Boolean,
    val position: Int = 0
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        colorHex = colorHex,
        type = NoteType.valueOf(type),
        tags = if (tags.isEmpty()) emptyList() else tags.split(","),
        folderId = folderId,
        imageUris = if (imageUris.isEmpty()) emptyList() else imageUris.split(","),
        audioPath = audioPath,
        sketchData = sketchData,
        createdAt = createdAt,
        lastModified = lastModified,
        reminderTimestamp = reminderTimestamp,
        isLocked = isLocked,
        position = position
    )

    companion object {
        fun fromDomain(note: Note): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            colorHex = note.colorHex,
            type = note.type.name,
            tags = note.tags.joinToString(","),
            folderId = note.folderId,
            imageUris = note.imageUris.joinToString(","),
            audioPath = note.audioPath,
            sketchData = note.sketchData,
            createdAt = note.createdAt,
            lastModified = note.lastModified,
            reminderTimestamp = note.reminderTimestamp,
            isLocked = note.isLocked,
            position = note.position
        )
    }
}
