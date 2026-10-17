package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SkyMindWeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
        // Trigger a fresh background fetch so the widget stays current without opening the app
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                WidgetDataSyncManager.refreshFromNetworkAndBroadcast(context)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    WidgetDataSyncManager.refreshFromNetworkAndBroadcast(context)
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.action.REFRESH_WEATHER_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appCtx = context.applicationContext
            val manager = AppWidgetManager.getInstance(appCtx)
            val componentName = ComponentName(appCtx, SkyMindWeatherWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            ids.forEach { id ->
                updateSingleWidget(appCtx, manager, id)
            }
        }

        fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val snap = WidgetDataSyncManager.loadSnapshot(context)
            val views = RemoteViews(context.packageName, R.layout.widget_skymind_weather)

            val emoji = WidgetDataSyncManager.weatherEmojiForCode(snap.weatherCode)
            val gpsTag = if (snap.isCurrentGps) " · 현위치" else ""
            views.setTextViewText(R.id.widget_location_text, "📍 ${snap.locationDisplayName}$gpsTag")
            views.setTextViewText(R.id.widget_umbrella_badge, "☔ ${snap.umbrellaStatus}")
            views.setTextViewText(R.id.widget_temp_text, "$emoji ${snap.currentTempC}°C")
            views.setTextViewText(
                R.id.widget_weather_summary,
                "${snap.weatherSummary} · 체감 ${snap.feelsLikeC}°C"
            )
            views.setTextViewText(
                R.id.widget_sub_metrics,
                "최고 ${snap.highTempC}° / 최저 ${snap.lowTempC}° · 습도 ${snap.humidityPercent}% · 미세먼지 ${snap.pm10Grade}"
            )
            views.setTextViewText(R.id.widget_outfit_title, "✨ ${snap.shortOutfitGuide}")
            views.setTextViewText(R.id.widget_outfit_outer_tag, "🧥 ${snap.outfitOuter}")
            views.setTextViewText(R.id.widget_outfit_top_tag, "👕 ${snap.outfitTop}")
            views.setTextViewText(R.id.widget_outfit_bottom_tag, "👖 ${snap.outfitBottom}")
            views.setTextViewText(R.id.widget_outfit_detail, snap.outfitDetail)

            // Launch MainActivity when clicking the widget container
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root_container, openAppPendingIntent)

            // Refresh widget weather & outfit in background without opening the app
            val refreshIntent = Intent(context, SkyMindWeatherWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun requestPinWidgetToHomeScreen(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val provider = ComponentName(context, SkyMindWeatherWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    return appWidgetManager.requestPinAppWidget(provider, null, null)
                }
            }
            return false
        }
    }
}
