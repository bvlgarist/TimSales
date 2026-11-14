
package com.timsales.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val prefs = context.getSharedPreferences(
            "timsales",
            Context.MODE_PRIVATE
        )

        val remindersEnabled = prefs.getBoolean(
            "remindersEnabled",
            true
        )

        val appointments = loadAppointments(context)

        scheduleAppointmentReminders(
            context,
            appointments,
            remindersEnabled
        )
    }
}
