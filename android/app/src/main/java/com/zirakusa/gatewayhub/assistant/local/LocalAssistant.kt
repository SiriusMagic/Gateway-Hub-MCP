package com.zirakusa.gatewayhub.assistant.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.gatewayhub.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the foreground conversation and Hub requests. Existing phone commands stay in Brain. */
class LocalAssistant(context: Context, private val scope: CoroutineScope) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("assistant", Context.MODE_PRIVATE)
    private val engine = LocalLanguageModel()
    private var turnJob: Job? = null
    private var history = emptyList<ChatMessage>()

    internal val phoneMcp = PhoneMcp(app)
    var tone by mutableStateOf(ConversationTone.entries.firstOrNull { it.name == prefs.getString("local_tone", null) } ?: ConversationTone.NATURAL)
        private set
    var chatEnabled by mutableStateOf(prefs.getBoolean("chat_enabled", false))
        private set
    var draft by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    val models = LocalModels(app, scope, canChange = { !busy }, onUnselected = { engine.close() })

    fun updateChatEnabled(enabled: Boolean) {
        chatEnabled = enabled
        prefs.edit { putBoolean("chat_enabled", enabled) }
        if (!enabled) {
            stop()
            history = emptyList()
            scope.launch { engine.close() }
        }
    }

    fun updateTone(value: ConversationTone) {
        tone = value
        prefs.edit { putString("local_tone", value.name) }
    }

    internal fun work(onFailure: (Int) -> Unit, block: suspend () -> Unit) {
        if (busy || turnJob?.isActive == true) return
        turnJob = scope.launch {
            busy = true
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkageError) {
                onFailure(R.string.local_model_error)
            } catch (error: Exception) {
                onFailure(when (error) {
                    is McpFailure -> error.hubMessage()
                    is PhoneMcpError -> error.status.label
                    else -> R.string.local_model_error
                })
            } finally {
                busy = false
                draft = null
            }
        }
    }

    internal suspend fun chat(query: String): String {
        check(chatEnabled)
        val messages = listOf(ChatMessage("system", "Eres Lucía, una asistente en Android. Responde brevemente en español. " +
            "No afirmes que has ejecutado acciones ni que tienes sentimientos propios. ${tone.instruction}")) +
            history + ChatMessage("user", query)
        draft = ""
        val response = generate(messages) { draft = it }.trim()
        check(response.isNotBlank())
        history = (history + ChatMessage("user", query) + ChatMessage("assistant", response)).takeLast(8)
        return response
    }

    internal fun stop() { turnJob?.cancel() }

    fun leave() = stop()

    fun close() {
        models.downloads.cancel()
        turnJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            try { engine.close() } finally { models.closeDocuments() }
        }
    }

    private suspend fun generate(
        messages: List<ChatMessage>, onText: suspend (String) -> Unit = {}
    ): String {
        val (template, file) = models.current() ?: throw UnsupportedOperationException()
        return engine.generate(template, file, messages) { withContext(Dispatchers.Main.immediate) { onText(it) } }
    }

}

/** The model's {"ordenes":[…]}: only short orders the rules understand, at most five. Pure. */
internal object OrderReply {
    fun parse(answer: String, understood: (String) -> Boolean): List<String> {
        val json = Regex("\\{[\\s\\S]*\\}").find(answer)?.value ?: return emptyList()
        val list = runCatching { org.json.JSONObject(json).optJSONArray("ordenes") ?: org.json.JSONObject(json).optJSONArray("orders") }.getOrNull()
            ?: return emptyList()
        return (0 until list.length()).mapNotNull { list.opt(it) as? String }
            .map { com.zirakusa.gatewayhub.assistant.Intents.clean(it).take(200) }
            .filter { it.isNotBlank() && understood(it) }.distinct().take(5)
    }
}
