package com.eldora25.tayfnotes.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eldora25.tayfnotes.data.dao.FolderDao
import com.eldora25.tayfnotes.data.dao.NoteDao
import com.eldora25.tayfnotes.data.entity.FolderEntity
import com.eldora25.tayfnotes.data.entity.NoteEntity

@Database(entities = [NoteEntity::class, FolderEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tayfnotes_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
