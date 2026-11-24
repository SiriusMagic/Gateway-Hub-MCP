package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lucía uses a scoped agent credential independently of the owner's administration session. */
class HubConnection(context: Context, private val scope: CoroutineScope, private val canChange: () -> Boolean) {
    private val sessions = KeystoreSessionStore(context, agent = true)
    private var linkJob: Job? = null
    internal var link: AgentLink? = null
        private set
    var hubAddress by mutableStateOf<String?>(null)
        private set
    var linkBusy by mutableStateOf(false)
        private set
    var linkMessage by mutableStateOf<Int?>(null)
        private set

    init {
        scope.launch {
            link = withContext(Dispatchers.IO) {
                sessions.load()?.let { (url, token) -> runCatching { AgentLink.restore(url, token) }.getOrNull() }
            }
            hubAddress = link?.endpoint?.host
        }.also { linkJob = it }
    }

    fun connect(raw: String, onConnected: () -> Unit) {
        if (linkJob?.isActive == true || !canChange()) return
        linkJob = scope.launch {
            linkBusy = true
            linkMessage = null
            try {
                val candidate = try { AgentLink.parse(raw) } catch (_: Exception) {
                    linkMessage = R.string.local_hub_link_invalid
                    return@launch
                }
                HubMcp(candidate).discover()
                withContext(Dispatchers.IO) { sessions.save(candidate.endpoint.toString(), candidate.token) }
                link = candidate
                hubAddress = candidate.endpoint.host
                onConnected()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                linkMessage = error.hubMessage()
            } finally {
                linkBusy = false
            }
        }
    }

    fun disconnect() {
        if (linkJob?.isActive == true || !canChange()) return
        sessions.clear()
        link = null
        hubAddress = null
        linkMessage = null
    }
}

internal fun Exception.hubMessage(): Int = when (this) {
    is McpFailure.Authorization -> R.string.local_hub_unauthorized
    is McpFailure.RateLimit -> R.string.local_hub_rate_limit
    is McpFailure.Tool -> R.string.local_hub_tool_error
    else -> R.string.local_hub_unavailable
}
