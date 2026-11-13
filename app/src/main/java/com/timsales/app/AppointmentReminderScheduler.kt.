
package com.timsales.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val REMINDER_BEFORE_MILLIS = 60 * 60 * 1000L

fun scheduleAppointmentReminders(
    context: Context,
    appointments: List<Appointment>,
    enabled: Boolean
) {
    val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val now = System.currentTimeMillis()

    appointments.forEach { appointment ->

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            putExtra("appointmentId", appointment.id)
            putExtra("client", appointment.client)
            putExtra("time", "${appointment.date} ${appointment.time}")
            putExtra("place", appointment.place)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Сначала отменяем прежнее напоминание.
        alarmManager.cancel(pendingIntent)

        if (!enabled || appointment.status != "Предстоящая") {
            return@forEach
        }

        val formatter = SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        ).apply {
            isLenient = false
            timeZone = TimeZone.getDefault()
        }

        val meetingTime = try {
            formatter.parse("${appointment.date} ${appointment.time}")?.time
        } catch (e: Exception) {
            null
        } ?: return@forEach

        val reminderTime = meetingTime - REMINDER_BEFORE_MILLIS

        // Если до встречи меньше часа — не ставим уведомление.
        if (reminderTime <= now) {
            return@forEach
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }
}
