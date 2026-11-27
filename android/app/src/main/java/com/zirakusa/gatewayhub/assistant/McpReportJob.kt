package com.zirakusa.gatewayhub.assistant

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal object McpReportQueue {
    const val PENDING = "pending"
    const val CONSENT = "sending"
    private const val JOB_ID = 8451
    private const val MAX_PENDING = 40

    fun preferences(context: Context) = context.getSharedPreferences("mcp_diagnostics", Context.MODE_PRIVATE)

    private fun queue(prefs: SharedPreferences) =
        runCatching { JSONArray(prefs.getString(PENDING, "[]")) }.getOrElse { JSONArray() }

    @Synchronized fun size(prefs: SharedPreferences) = queue(prefs).length()

    @Synchronized fun allow(prefs: SharedPreferences, value: Boolean) {
        prefs.edit {
            putBoolean(CONSENT, value)
            if (!value) remove(PENDING)
        }
    }

    @Synchronized fun add(prefs: SharedPreferences, report: JSONObject) {
        if (!prefs.getBoolean(CONSENT, false)) return
        val items = queue(prefs)
        if (items.length() >= MAX_PENDING) items.remove(0)
        prefs.edit { putString(PENDING, items.put(report).toString()) }
    }

    @Synchronized fun next(prefs: SharedPreferences): JSONObject? =
        if (prefs.getBoolean(CONSENT, false)) queue(prefs).optJSONObject(0) else null

    @Synchronized fun delivered(prefs: SharedPreferences, id: String) {
        val items = queue(prefs)
        if (items.optJSONObject(0)?.optString("id") != id) return
        items.remove(0)
        // The Hub deduplicates the ID if Android stops after sending but before saving this acknowledgement.
        prefs.edit { putString(PENDING, items.toString()) }
    }

    fun updateSchedule(context: Context, prefs: SharedPreferences) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!prefs.getBoolean(CONSENT, false)) {
            scheduler.cancel(JOB_ID)
        } else if (size(prefs) != 0 && scheduler.getPendingJob(JOB_ID) == null) {
            val request = JobInfo.Builder(JOB_ID, ComponentName(context, McpReportJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(30_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setPersisted(true)
                .build()
            scheduler.schedule(request)
        }
    }
}

/** Retries envelopes only. The Android scheduler can send a queued failure even with Luna closed. */
class McpReportJob : JobService() {
    private var running: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        running = CoroutineScope(Dispatchers.Main.immediate).launch {
            val retry = withContext(Dispatchers.IO) { deliver() }
            running = null
            jobFinished(params, retry)
            if (!retry) McpReportQueue.updateSchedule(this@McpReportJob, McpReportQueue.preferences(this@McpReportJob))
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        running = null
        return McpReportQueue.next(McpReportQueue.preferences(this)) != null
    }

    private suspend fun deliver(): Boolean {
        val prefs = McpReportQueue.preferences(this)
        while (true) {
            val report = McpReportQueue.next(prefs) ?: return false
            val (url, token) = KeystoreSessionStore(this).load() ?: return true
            try {
                HubApi(url, token).reportMcpFailure(report)
                McpReportQueue.delivered(prefs, report.getString("id"))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return true
            }
        }
    }
}
