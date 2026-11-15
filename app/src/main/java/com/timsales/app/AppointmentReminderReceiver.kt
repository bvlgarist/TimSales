package com.timsales.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appointmentId = intent.getIntExtra("appointmentId", 0)
        val client = intent.getStringExtra("client") ?: "Клиент"
        val time = intent.getStringExtra("time") ?: ""
        val place = intent.getStringExtra("place") ?: ""

        if (appointmentId == 0) return

        when (intent.action) {
            ACTION_COMPLETE_APPOINTMENT -> {
                markAppointmentCompleted(context, appointmentId)
                cancelAppointmentReminders(context, appointmentId)
                return
            }
            ACTION_SNOOZE_APPOINTMENT -> {
                scheduleSnoozedAppointmentReminder(
                    context, appointmentId, client, time, place
                )
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(intent.getIntExtra("notificationId", 0))
                return
            }
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val reminderType = intent.getIntExtra("reminderType", REMINDER_TYPE_1_HOUR)
        val notificationId = appointmentId * 100 + reminderType
        val channelId = "appointment_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания о встречах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о запланированных встречах TimSales"
            }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_APPOINTMENT
            putExtra("appointmentId", appointmentId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId * 100 + 40,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_APPOINTMENT
            putExtra("appointmentId", appointmentId)
            putExtra("client", client)
            putExtra("time", time)
            putExtra("place", place)
            putExtra("notificationId", notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId * 100 + 41,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (reminderType) {
            REMINDER_TYPE_12_HOURS -> "Встреча через 12 часов: $client"
            REMINDER_TYPE_SNOOZE -> "Напоминание о встрече: $client"
            else -> "Встреча через 1 час: $client"
        }

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$time • $place")
            .setStyle(
                Notification.BigTextStyle()
                    .bigText("Клиент: $client\nВремя: $time\nМесто: $place")
            )
            .setContentIntent(contentPendingIntent)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Завершить",
                    completePendingIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_popup_reminder,
                    "Через 10 минут",
                    snoozePendingIntent
                ).build()
            )
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun markAppointmentCompleted(context: Context, appointmentId: Int) {
        val prefs = context.getSharedPreferences("timsales", Context.MODE_PRIVATE)
        val source = prefs.getString("appointments", "[]") ?: "[]"
        try {
            val appointments = JSONArray(source)
            for (index in 0 until appointments.length()) {
                val appointment = appointments.optJSONObject(index) ?: continue
                if (appointment.optInt("id") == appointmentId) {
                    appointment.put("status", "Завершена")
                    prefs.edit().putString("appointments", appointments.toString()).commit()
                    return
                }
            }
        } catch (_: Exception) {
            // Keep the app data untouched if the stored JSON is malformed.
        }
    }
}
