package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MorningBriefingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val config = MorningBriefingScheduler.loadConfig(context)
        if (config.enabled) {
            // Schedule next day's morning alarm first
            MorningBriefingScheduler.scheduleNextDailyAlarm(context, config.hour, config.minute)
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                MorningBriefingScheduler.fetchAndShowMorningBriefingNotification(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MORNING_BRIEFING_ALARM = "com.example.action.MORNING_BRIEFING_ALARM"
    }
}

class MorningBriefingBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val config = MorningBriefingScheduler.loadConfig(context)
            if (config.enabled) {
                MorningBriefingScheduler.ensureNotificationChannel(context)
                MorningBriefingScheduler.scheduleNextDailyAlarm(context, config.hour, config.minute)
            }
        }
    }
}
