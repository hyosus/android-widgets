package com.example.weatherapp
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.weatherapp.weatherApi.NetworkResponse

class CloudWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle both default widget update and custom refresh action
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
            REFRESH_ACTION -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
        }
    }

    companion object {
        // Action for refresh intent
        const val REFRESH_ACTION = "com.example.weatherapp.REFRESH_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.cloud_widget)

            // Create a pending intent for the refresh button
            val refreshIntent = Intent(context, CloudWidget::class.java).apply {
                action = REFRESH_ACTION
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }

            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set the refresh button click listener
            views.setOnClickPendingIntent(R.id.refreshBtn, refreshPendingIntent)

            // Create ViewModel directly
            val viewModel = WeatherViewModel()
            viewModel.getData("Singapore") // Default city

            // Observe the LiveData manually
            viewModel.weatherResult.observeForever { result ->
                when (result) {
                    is NetworkResponse.Success -> {
                        Log.d("CloudWidget", "Cloud: ${result.data.current.cloud}%, Lat=${result.data.location.lat}, Lon=${result.data.location.lon}")
                        views.setTextViewText(
                            R.id.cloudPercentText,
                            "${result.data.current.cloud}%"
                        )

                        if (result.data.current.cloud < 15.toString()) {
                            views.setTextViewText(
                                R.id.cloudDescText, "Mostly Clear"
                            )
                        }
                        else if (result.data.current.cloud > 15.toString() && result.data.current.cloud < 50.toString()) {
                            views.setTextViewText(
                                R.id.cloudDescText, "Partly Cloudy"
                            )
                        }
                        else {
                            views.setTextViewText(
                                R.id.cloudDescText, "Mostly Cloudy"
                            )
                        }
                    }
                    is NetworkResponse.Error -> {
                        views.setTextViewText(
                            R.id.cloudPercentText,
                            "Error: ${result.exception.message}"
                        )
                    }
                    NetworkResponse.Loading -> {
                        views.setTextViewText(
                            R.id.cloudPercentText,
                            "Loading..."
                        )
                    }
                }

                // Update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}