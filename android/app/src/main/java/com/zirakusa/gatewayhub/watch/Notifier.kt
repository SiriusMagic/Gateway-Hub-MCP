package com.zirakusa.gatewayhub.watch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.zirakusa.gatewayhub.MainActivity
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.ENV_DISCONNECTED
import com.zirakusa.gatewayhub.data.ENV_PAUSED

/** The three kinds of notification: "MCP active" (persistent), periodic summary, immediate alert. */
class Notifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(NotificationChannel(ACTIVE, context.getString(R.string.channel_active), NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(SUMMARY, context.getString(R.string.channel_summary), NotificationManager.IMPORTANCE_DEFAULT))
        manager.createNotificationChannel(NotificationChannel(ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH))
    }

    private fun open() = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE)

    private fun action(state: String, label: Int, code: Int) = Notification.Action.Builder(null, context.getString(label),
        PendingIntent.getService(context, code, Intent(context, HubWatchService::class.java).setAction(HubWatchService.ACTION_ENVIRONMENT)
            .putExtra(HubWatchService.EXTRA_STATE, state), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()

    private fun builder(channel: String, title: String, text: String) = Notification.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_hub).setContentTitle(title).setContentText(text)
        .setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open())

    /** Persistent while the background mode is on: it shows the service exists and stops it in one tap. */
    fun active(): Notification = builder(ACTIVE, context.getString(R.string.notif_active_title), context.getString(R.string.notif_active_text))
        .setOngoing(true).addAction(action(ENV_PAUSED, R.string.env_pause, 1)).addAction(action(ENV_DISCONNECTED, R.string.env_disconnect, 2)).build()

    fun summary(calls: Int, errors: Int, agents: List<String>) = post(SUMMARY_ID, builder(SUMMARY, context.getString(R.string.notif_summary_title),
        context.resources.getQuantityString(R.plurals.notif_summary_text, calls, calls, errors,
            agents.joinToString(", ").ifEmpty { "—" })).setAutoCancel(true).build())

    fun alert(errors: Int) = post(ALERT_ID, builder(ALERTS, context.getString(R.string.notif_alert_title),
        context.resources.getQuantityString(R.plurals.notif_alert_text, errors, errors)).setAutoCancel(true).build())

    fun environment(state: String) = post(ALERT_ID, builder(SUMMARY, context.getString(R.string.app_name),
        context.getString(if (state == ENV_PAUSED) R.string.env_note_paused else R.string.env_note_disconnected)).setAutoCancel(true).build())

    private fun post(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        manager.notify(id, notification)
    }

    companion object {
        const val ACTIVE = "mcp_active"; const val SUMMARY = "mcp_summary"; const val ALERTS = "mcp_alerts"
        const val ACTIVE_ID = 1; const val SUMMARY_ID = 2; const val ALERT_ID = 3
    }
}
