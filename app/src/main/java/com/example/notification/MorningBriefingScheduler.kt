package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.CityLocation
import com.example.repository.WeatherRepository
import com.example.widget.WidgetDataSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class MorningBriefingConfig(
    val enabled: Boolean = true,
    val hour: Int = 7,
    val minute: Int = 30
) {
    val formattedTimeKo: String
        get() {
            val amPm = if (hour < 12) "오전" else "오후"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format(Locale.KOREA, "%s %02d:%02d", amPm, displayHour, minute)
        }
}

object MorningBriefingScheduler {
    private const val PREF_NAME = "skymind_morning_briefing_prefs"
    private const val KEY_ENABLED = "morning_briefing_enabled"
    private const val KEY_HOUR = "morning_briefing_hour"
    private const val KEY_MINUTE = "morning_briefing_minute"

    const val CHANNEL_ID = "skymind_morning_briefing_channel"
    private const val NOTIFICATION_ID = 7301
    private const val ALARM_REQUEST_CODE = 7302

    fun loadConfig(context: Context): MorningBriefingConfig {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return MorningBriefingConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            hour = prefs.getInt(KEY_HOUR, 7).coerceIn(0, 23),
            minute = prefs.getInt(KEY_MINUTE, 30).coerceIn(0, 59)
        )
    }

    fun saveAndSchedule(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ): MorningBriefingConfig {
        val appCtx = context.applicationContext
        val cleanHour = hour.coerceIn(0, 23)
        val cleanMinute = minute.coerceIn(0, 59)
        appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, cleanHour)
            .putInt(KEY_MINUTE, cleanMinute)
            .apply()

        val config = MorningBriefingConfig(enabled, cleanHour, cleanMinute)
        ensureNotificationChannel(appCtx)

        if (enabled) {
            scheduleNextDailyAlarm(appCtx, cleanHour, cleanMinute)
        } else {
            cancelScheduledAlarm(appCtx)
        }
        return config
    }

    fun scheduleNextDailyAlarm(context: Context, hour: Int, minute: Int) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appCtx, MorningBriefingAlarmReceiver::class.java).apply {
            action = MorningBriefingAlarmReceiver.ACTION_MORNING_BRIEFING_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appCtx,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now) || timeInMillis <= now.timeInMillis + 5_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Use setAndAllowWhileIdle (no restricted exact alarm permission required, Play Policy compliant)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            target.timeInMillis,
            pendingIntent
        )
    }

    fun cancelScheduledAlarm(context: Context) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(appCtx, MorningBriefingAlarmReceiver::class.java).apply {
            action = MorningBriefingAlarmReceiver.ACTION_MORNING_BRIEFING_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appCtx,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "아침 날씨 & 옷차림 외출 브리핑",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "매일 아침 사용자가 설정한 시간에 당일 날씨, 추천 옷차림, 우산 필요 여부를 알려드립니다."
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun fetchAndShowMorningBriefingNotification(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        ensureNotificationChannel(appCtx)
        if (!hasNotificationPermission(appCtx)) {
            return@withContext false
        }

        // Refresh latest weather & outfit snapshot if network is available
        try {
            val snapBefore = WidgetDataSyncManager.loadSnapshot(appCtx)
            val repo = WeatherRepository()
            val location = CityLocation(
                name = snapBefore.locationName,
                latitude = snapBefore.latitude,
                longitude = snapBefore.longitude,
                isCurrentGps = snapBefore.isCurrentGps
            )
            val result = repo.fetchWeatherBundle(location)
            result.onSuccess { bundle ->
                val advice = repo.generateGeminiAdvice(bundle, "의상 · 일상 캐주얼")
                WidgetDataSyncManager.saveAndBroadcast(appCtx, bundle, advice)
            }
        } catch (_: Exception) {
        }

        val snap = WidgetDataSyncManager.loadSnapshot(appCtx)
        val emoji = WidgetDataSyncManager.weatherEmojiForCode(snap.weatherCode)

        val title = "$emoji [${snap.locationName}] 오늘 아침 날씨 & 옷차림 브리핑"
        val shortSummary = "현재 ${snap.currentTempC}°(최고 ${snap.highTempC}°/최저 ${snap.lowTempC}°) · ☔ ${snap.umbrellaStatus} · ${snap.outfitTitle.removePrefix("🧥 오늘의 옷차림 · ")}"
        val expandedText = buildString {
            appendLine("• 오늘 날씨: ${snap.weatherSummary} · 현재 ${snap.currentTempC}°C (체감 ${snap.feelsLikeC}°C, 최고 ${snap.highTempC}° / 최저 ${snap.lowTempC}°)")
            appendLine("• 우산 & 대기: ☔ ${snap.umbrellaStatus} · 습도 ${snap.humidityPercent}% · 기압 ${snap.pressureHpa}hPa · 미세먼지 ${snap.pm10Grade}")
            appendLine("• 추천 옷차림: ${snap.outfitTitle.removePrefix("🧥 ")}")
            append("• 코디 디테일: ${snap.outfitDetail}")
        }

        val openAppIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appCtx,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return@withContext try {
            NotificationManagerCompat.from(appCtx).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
