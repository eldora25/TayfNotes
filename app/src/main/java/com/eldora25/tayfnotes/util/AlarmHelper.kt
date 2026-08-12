package com.eldora25.tayfnotes.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.eldora25.tayfnotes.receiver.ReminderReceiver
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.RepeatInterval

object AlarmHelper {
    fun scheduleReminder(context: Context, note: Note) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("NOTE_ID", note.id)
            putExtra("NOTE_TITLE", note.title)
            putExtra("NOTE_CONTENT", note.content.take(50))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, note.id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        note.reminderTimestamp?.let { timestamp ->
            if (timestamp > System.currentTimeMillis()) {
                val interval = when(note.reminderRepeat) {
                    RepeatInterval.DAILY -> AlarmManager.INTERVAL_DAY
                    RepeatInterval.WEEKLY -> AlarmManager.INTERVAL_DAY * 7
                    else -> 0L
                }

                if (interval > 0) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, timestamp, interval, pendingIntent)
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
                    }
                }
            }
        }
    }

    fun scheduleItemReminder(context: Context, noteId: String, itemId: String, title: String, timestamp: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("NOTE_ID", noteId)
            putExtra("ITEM_ID", itemId)
            putExtra("NOTE_TITLE", title)
            putExtra("NOTE_CONTENT", "Kontrol listesi hatırlatıcısı")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, (noteId + itemId).hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (timestamp > System.currentTimeMillis()) {
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
            }
        }
    }

    fun cancelReminder(context: Context, noteId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
