package com.eldora25.tayfnotes.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eldora25.tayfnotes.shared.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val position: Int = 0
) {
    fun toDomain(): Folder = Folder(
        id = id,
        name = name,
        colorHex = colorHex,
        createdAt = createdAt,
        lastModified = lastModified,
        position = position
    )

    companion object {
        fun fromDomain(folder: Folder): FolderEntity = FolderEntity(
            id = folder.id,
            name = folder.name,
            colorHex = folder.colorHex,
            createdAt = folder.createdAt,
            lastModified = folder.lastModified,
            position = folder.position
        )
    }
}
