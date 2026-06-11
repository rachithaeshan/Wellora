package com.example.wellora

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, HabitWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.wellora.UPDATE_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                val habitDao = AppDatabase.getInstance(context).habitDao()
                val habits = habitDao.getAllHabits()

                val completed = habits.count { it.isCompleted }
                val total = habits.size
                val percentage = if (total == 0) 0 else (completed * 100) / total

                val views = RemoteViews(context.packageName, R.layout.widget_habit)
                views.setTextViewText(R.id.widgetPercentage, "$percentage%")
                views.setTextViewText(R.id.widgetProgress, "$completed of $total completed")
                views.setProgressBar(R.id.widgetProgressBar, 100, percentage, false)

                // Click to open app
                val intent = Intent(context, HomeActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun updateWidget(context: Context) {
            val intent = Intent(context, HabitWidgetProvider::class.java)
            intent.action = ACTION_UPDATE_WIDGET
            context.sendBroadcast(intent)
        }
    }
}