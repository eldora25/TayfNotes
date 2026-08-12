package com.eldora25.tayfnotes.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.eldora25.tayfnotes.MainActivity
import com.eldora25.tayfnotes.R
import com.eldora25.tayfnotes.receiver.NotificationActionReceiver

class NotificationHelper(private val context: Context) {
    private val channelId = "tayfnotes_reminders"
    private val channelName = "TayfNotes Hatırlatıcılar"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Not hatırlatıcı bildirimleri"
                enableLights(true)
                enableVibration(true)
                setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(noteId: String, noteTitle: String, noteContent: String) {
        val notificationId = noteId.hashCode()
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_NOTE_ID", noteId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Actions
        val completedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_COMPLETED"
            putExtra("NOTE_ID", noteId)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val completedPending = PendingIntent.getBroadcast(context, notificationId + 1, completedIntent, PendingIntent.FLAG_IMMUTABLE)

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("NOTE_ID", noteId)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val snoozePending = PendingIntent.getBroadcast(context, notificationId + 2, snoozeIntent, PendingIntent.FLAG_IMMUTABLE)

        val closeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_CLOSE"
            putExtra("NOTE_ID", noteId)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val closePending = PendingIntent.getBroadcast(context, notificationId + 3, closeIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(noteTitle)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // Makes it more persistent/intrusive
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true) // Keeps it in tray until actioned
            .addAction(0, "Tamamlandı", completedPending)
            .addAction(0, "Ertele", snoozePending)
            .addAction(0, "Kapat", closePending)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
