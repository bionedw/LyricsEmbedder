package com.lyricsembedder.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

class NotificationHelper(private val context: Context) {
    private val nextNotificationId = AtomicInteger(2000)

    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Yêu cầu lời bài hát",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo trạng thái khi yêu cầu lời bài hát thất bại"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyLyricsFailure(
        trackTitle: String,
        reason: String,
        replacePrevious: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Không tìm thấy lời bài hát")
            .setContentText("$trackTitle — $reason")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val id = if (replacePrevious) SINGLE_NOTIFICATION_ID else nextNotificationId.incrementAndGet()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "lyrics_request_channel"
        private const val SINGLE_NOTIFICATION_ID = 1001
    }
}
