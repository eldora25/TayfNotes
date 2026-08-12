package com.eldora25.tayfnotes.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.eldora25.tayfnotes.data.database.AppDatabase
import com.eldora25.tayfnotes.data.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("NOTE_ID") ?: return
        val action = intent.action
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", noteId.hashCode())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            "ACTION_COMPLETED" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val repository = NoteRepository(db.noteDao())
                    val notesList = repository.allNotes.first()
                    val note = notesList.find { it.id == noteId }
                    if (note != null) {
                        repository.insert(note.copy(reminderTimestamp = null))
                    }
                }
                notificationManager.cancel(notificationId)
                Toast.makeText(context, "Görev tamamlandı", Toast.LENGTH_SHORT).show()
            }
            "ACTION_SNOOZE" -> {
                notificationManager.cancel(notificationId)
                Toast.makeText(context, "15 dakika ertelendi", Toast.LENGTH_SHORT).show()
            }
            "ACTION_CLOSE" -> {
                notificationManager.cancel(notificationId)
            }
        }
    }
}
