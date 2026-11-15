package com.timsales.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                val prefs = context.getSharedPreferences(
                    "timsales",
                    Context.MODE_PRIVATE
                )
                val remindersEnabled = prefs.getBoolean("remindersEnabled", true)
                val appointments = loadAppointments(context)
                scheduleAppointmentReminders(
                    context,
                    appointments,
                    remindersEnabled
                )
            }
        }
    }
}
