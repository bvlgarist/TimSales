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

private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L
private const val SNOOZE_MILLIS = 10L * 60L * 1000L

internal const val REMINDER_TYPE_1_HOUR = 1
internal const val REMINDER_TYPE_SNOOZE = 2

internal const val ACTION_COMPLETE_APPOINTMENT = "com.timsales.app.ACTION_COMPLETE_APPOINTMENT"
internal const val ACTION_SNOOZE_APPOINTMENT = "com.timsales.app.ACTION_SNOOZE_APPOINTMENT"

private fun reminderRequestCode(appointmentId: Int, reminderType: Int): Int =
    appointmentId * 100 + reminderType

private fun reminderPendingIntent(
    context: Context,
    appointment: Appointment,
    reminderType: Int
): PendingIntent {
    val requestCode = reminderRequestCode(appointment.id, reminderType)
    val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
        putExtra("appointmentId", appointment.id)
        putExtra("client", appointment.client)
        putExtra("time", "${appointment.date} ${appointment.time}")
        putExtra("place", appointment.place)
        putExtra("reminderType", reminderType)
    }
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun setReminderAlarm(
    context: Context,
    triggerAtMillis: Long,
    pendingIntent: PendingIntent
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        alarmManager.canScheduleExactAlarms()
    ) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    } else {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}

internal fun cancelAppointmentReminders(context: Context, appointmentId: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    listOf(
        REMINDER_TYPE_1_HOUR,
        REMINDER_TYPE_SNOOZE,
        3
    ).forEach { type ->
        val requestCode = reminderRequestCode(appointmentId, type)
        val intent = Intent(context, AppointmentReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .cancel(requestCode)
    }
}

internal fun scheduleSnoozedAppointmentReminder(
    context: Context,
    appointmentId: Int,
    client: String,
    time: String,
    place: String
) {
    val appointment = Appointment(
        id = appointmentId,
        client = client,
        date = "",
        time = time,
        place = place,
        products = ""
    )
    val pendingIntent = reminderPendingIntent(
        context,
        appointment,
        REMINDER_TYPE_SNOOZE
    )
    setReminderAlarm(
        context,
        System.currentTimeMillis() + SNOOZE_MILLIS,
        pendingIntent
    )
}

fun scheduleAppointmentReminders(
    context: Context,
    appointments: List<Appointment>,
    enabled: Boolean
) {
    val formatter = SimpleDateFormat(
        "dd.MM.yyyy HH:mm",
        Locale.getDefault()
    ).apply {
        isLenient = false
        timeZone = TimeZone.getDefault()
    }

    val now = System.currentTimeMillis()

    appointments.forEach { appointment ->
        cancelAppointmentReminders(context, appointment.id)

        if (!enabled || appointment.status != "Предстоящая") {
            return@forEach
        }

        val meetingTime = try {
            formatter.parse("${appointment.date} ${appointment.time}")?.time
        } catch (_: Exception) {
            null
        } ?: return@forEach

        val reminderTime = meetingTime - ONE_HOUR_MILLIS
        if (reminderTime > now) {
            setReminderAlarm(
                context,
                reminderTime,
                reminderPendingIntent(context, appointment, REMINDER_TYPE_1_HOUR)
            )
        }
    }
}
