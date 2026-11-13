
package com.timsales.app

import android.Manifest
import android.content.BroadcastReceiver
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val client = intent.getStringExtra("client") ?: "Клиент"
        val time = intent.getStringExtra("time") ?: ""
        val place = intent.getStringExtra("place") ?: ""

        val channelId = "appointment_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания о встречах",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Скоро встреча: $client")
            .setContentText("$time • $place")
            .setStyle(
                Notification.BigTextStyle()
                    .bigText("Клиент: $client\nВремя: $time\nМесто: $place")
            )
            .setAutoCancel(true)
            .build()

        manager.notify(intent.getIntExtra("appointmentId", 0), notification)
    }
}
