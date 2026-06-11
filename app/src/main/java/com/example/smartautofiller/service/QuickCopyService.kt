package com.example.smartautofiller.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.smartautofiller.R
import com.example.smartautofiller.data.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class QuickCopyService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val CHANNEL_ID = "QuickCopyChannel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        updateNotification()
        return START_STICKY
    }

    private fun updateNotification() {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(this@QuickCopyService)
            val profile = db.userProfileDao().getAllProfiles().first().firstOrNull()
            
            if (profile != null) {
                val notification = NotificationCompat.Builder(this@QuickCopyService, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_edit)
                    .setContentTitle("Smart Autofill Active: ${profile.profileName}")
                    .setContentText("Tap to copy your details")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .addAction(createCopyAction("Name", profile.fullName))
                    .addAction(createCopyAction("Phone", profile.phoneNumber))
                    .addAction(createCopyAction("Email", profile.email))
                    .build()

                startForeground(1, notification)
            }
        }
    }

    private fun createCopyAction(label: String, text: String): NotificationCompat.Action {
        val intent = Intent(this, CopyReceiver::class.java).apply {
            putExtra("text", text)
            putExtra("label", label)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, label.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(0, label, pendingIntent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Quick Copy", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
