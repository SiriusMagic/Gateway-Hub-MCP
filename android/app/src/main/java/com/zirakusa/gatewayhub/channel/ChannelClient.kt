package com.zirakusa.gatewayhub.channel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The phone's outgoing WebSocket to `wss://<hub>/channel`, with this phone's credential in the header
 * (never in the URL). Reconnects with growing pauses; a revoked phone (close code 4001 or 401) stops trying.
 */
class ChannelClient(
    private val hubUrl: String,
    private val secret: String,
    private val onOpen: (WebSocket) -> Unit,
    private val onText: (String) -> Unit,
    private val onRevoked: () -> Unit,
) {
    enum class State { OFFLINE, CONNECTING, ONLINE }
    private val mutable = MutableStateFlow(State.OFFLINE)
    val state: StateFlow<State> = mutable
    private val http = OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).readTimeout(0, TimeUnit.MILLISECONDS).build()
    private val retry = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var running = false
    private var attempt = 0

    fun start() { if (running) return; running = true; attempt = 0; connect() }

    fun stop() {
        running = false
        socket?.close(1000, "off"); socket = null
        mutable.value = State.OFFLINE
    }

    fun send(text: String): Boolean = socket?.send(text) == true

    private fun connect() {
        if (!running) return
        mutable.value = State.CONNECTING
        val request = Request.Builder().url(hubUrl.replaceFirst("https://", "wss://") + "/channel")
            .header("Authorization", "Bearer $secret").build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { attempt = 0; mutable.value = State.ONLINE; onOpen(webSocket) }
            override fun onMessage(webSocket: WebSocket, text: String) = onText(text)
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = lost(code)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = lost(response?.code ?: 0)
        })
    }

    private fun lost(code: Int) {
        mutable.value = State.OFFLINE
        if (code == 4001 || code == 401) { running = false; onRevoked(); return }
        if (!running) return
        val pause = listOf(2L, 5L, 15L, 30L, 60L)[minOf(attempt++, 4)]
        retry.schedule({ connect() }, pause, TimeUnit.SECONDS)
    }
}
