package com.eldora25.tayfnotes.data.repository

import com.eldora25.tayfnotes.data.dao.NoteDao
import com.eldora25.tayfnotes.data.entity.NoteEntity
import com.eldora25.tayfnotes.shared.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    fun search(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%").map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getNotesByFolder(folderId: String): Flow<List<Note>> {
        return noteDao.getNotesByFolder(folderId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun insert(note: Note) {
        noteDao.insertNote(NoteEntity.fromDomain(note))
    }

    suspend fun updateNotes(notes: List<Note>) {
        notes.forEach { note ->
            noteDao.insertNote(NoteEntity.fromDomain(note))
        }
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(NoteEntity.fromDomain(note))
    }
}
