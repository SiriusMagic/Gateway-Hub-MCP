package com.zirakusa.gatewayhub.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.zirakusa.gatewayhub.R
import org.vosk.Recognizer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * «Oye Lucía»: listens for the name while the user turned it on, with a fixed notification (Android requires it for the
 * microphone). Only sound clearly above the room's noise is decoded ([EnergyGate]) and only against a tiny grammar
 * ([WakeName]), so it stays light; with «solo mientras carga» it pauses on battery. On the phrase it opens Lucía, which
 * takes the microphone; listening for the name starts again when Lucía closes.
 */
class WakeService : Service() {
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    private val power = object : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) = restart() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs(this).wake || !Vosk.canListen(this)) { stopSelf(); return START_NOT_STICKY }
        val type = if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        runCatching { ServiceCompat.startForeground(this, ID, notification(), type) }.onFailure { stopSelf(); return START_NOT_STICKY }
        ContextCompat.registerReceiver(this, power, IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED) },
            ContextCompat.RECEIVER_NOT_EXPORTED)
        restart()
        return START_STICKY
    }

    private fun charging() = (getSystemService(BatteryManager::class.java)?.isCharging) == true

    private fun restart() {
        stopListening()
        if (paused || (Prefs(this).onlyCharging && !charging())) return
        running.set(true)
        Vosk.model(this) { model -> if (model != null && running.get()) thread = Thread({ listen(model) }, "lucia-wake").apply { start() } }
    }

    private fun listen(model: org.vosk.Model) {
        val name = Prefs(this).name
        val mic = Vosk.microphone() ?: return
        val r = Recognizer(model, Vosk.RATE.toFloat(), name.grammar())
        val gate = EnergyGate()
        val buffer = ShortArray(Vosk.RATE / 10)
        try {
            mic.startRecording()
            while (running.get()) {
                val n = mic.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                if (!gate.open(gate.rms(buffer, n))) continue      // silence: nothing to decode
                val text = if (r.acceptWaveForm(buffer, n)) Vosk.text(r.result) else Vosk.text(r.partialResult, "partial")
                if (name.heard(text)) { r.reset(); running.set(false); wake(this); break }
            }
        } finally { runCatching { mic.stop() }; mic.release(); r.close() }
    }

    private fun stopListening() { running.set(false); thread?.join(500); thread = null }

    override fun onDestroy() { stopListening(); runCatching { unregisterReceiver(power) }; instance = null; super.onDestroy() }
    override fun onCreate() { super.onCreate(); instance = this }

    private fun notification(): Notification {
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.wake_channel), NotificationManager.IMPORTANCE_MIN))
        val open = PendingIntent.getActivity(this, 0, Intent(this, AssistantActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_stat_lucia)
            .setContentTitle(getString(R.string.wake_listening, Prefs(this).name.shown)).setContentText(getString(R.string.wake_listening_text))
            .setOngoing(true).setContentIntent(open).setCategory(NotificationCompat.CATEGORY_SERVICE).build()
    }

    companion object {
        private const val ID = 7; private const val CHANNEL = "wake"; private const val CALL = "call"
        @Volatile private var instance: WakeService? = null
        /** While Lucía is open she owns the microphone. */
        @Volatile var paused = false
            set(v) { field = v; instance?.restart() }

        fun start(context: Context) { if (Prefs(context).wake) ContextCompat.startForegroundService(context, Intent(context, WakeService::class.java)) }
        fun stop(context: Context) = context.stopService(Intent(context, WakeService::class.java))

        /**
         * Opens Lucía listening. As the phone's assistant, Android lets her open herself; with «aparecer encima» too;
         * otherwise a notification to tap (Android doesn't let apps jump in front by themselves).
         */
        fun wake(context: Context) {
            val listen = Intent(context, AssistantActivity::class.java).setAction(AssistantActivity.LISTEN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (LuciaService.show(Bundle().apply { putBoolean(AssistantActivity.LISTEN, true) })) return
            if (Settings.canDrawOverlays(context) && runCatching { context.startActivity(listen); true }.getOrDefault(false)) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(NotificationChannel(CALL, context.getString(R.string.wake_call_channel), NotificationManager.IMPORTANCE_HIGH))
            val tap = PendingIntent.getActivity(context, 1, listen, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val n = NotificationCompat.Builder(context, CALL).setSmallIcon(R.drawable.ic_stat_lucia).setContentTitle(context.getString(R.string.wake_heard))
                .setContentText(context.getString(R.string.wake_tap)).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(tap, true).setContentIntent(tap).setAutoCancel(true).setTimeoutAfter(15_000).build()
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) runCatching { nm.notify(8, n) }
        }
    }
}
