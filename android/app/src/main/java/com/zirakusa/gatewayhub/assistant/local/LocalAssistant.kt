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
    val hub = HubConnection(app, scope) { !busy && pending == null }
    val models = LocalModels(app, scope, canChange = { !busy && pending == null }, onUnselected = { engine.close() })
    var tone by mutableStateOf(ConversationTone.entries.firstOrNull { it.name == prefs.getString("local_tone", null) } ?: ConversationTone.NATURAL)
        private set
    var draft by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var canStop by mutableStateOf(false)
        private set
    var pending by mutableStateOf<Pending?>(null)
        private set

    fun updateTone(value: ConversationTone) {
        tone = value
        prefs.edit { putString("local_tone", value.name) }
    }

    internal fun work(onFailure: (Int) -> Unit, cancellable: Boolean = true, block: suspend () -> Unit) {
        if (busy || turnJob?.isActive == true) return
        turnJob = scope.launch {
            busy = true
            canStop = cancellable
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
                canStop = false
                draft = null
            }
        }
    }

    internal suspend fun chat(query: String): String {
        val messages = listOf(ChatMessage("system", "Eres Lucía, una asistente en Android. Responde brevemente en español. " +
            "No afirmes que has ejecutado acciones ni que tienes sentimientos propios. ${tone.instruction}")) +
            history + ChatMessage("user", query)
        draft = ""
        val response = generate(messages) { draft = it }.trim()
        check(response.isNotBlank())
        history = (history + ChatMessage("user", query) + ChatMessage("assistant", response)).takeLast(8)
        return response
    }

    internal suspend fun decide(query: String): Pending? {
        val client = HubMcp(hub.link ?: throw McpFailure.Authorization())
        val selection = choose(query, client.discoverOrFail()) ?: return null
        return Pending(client, selection)
    }

    internal suspend fun askStone(query: String): String? {
        val app = phoneMcp.stone()
        if (app.status != PhoneMcpStatus.READY) throw PhoneMcpError(app.status)
        val selection = choose(query, app.tools) ?: return null
        val answer = StoneRequests.answer(selection.tool.name, phoneMcp.call(app, selection), this.app.resources)
        history = (history + ChatMessage("user", query) + ChatMessage("assistant", answer)).takeLast(8)
        return answer
    }

    private suspend fun choose(query: String, tools: List<McpTool>): McpSelection? {
        val candidates = McpSelection.candidates(query, tools)
        if (candidates.isEmpty()) return null
        val instruction = "Selecciona una herramienta que coincida con la petición en español. " +
            "Devuelve exclusivamente JSON con las claves tool (nombre exacto) y arguments (objeto). " +
            "Si ninguna herramienta coincide o faltan datos obligatorios, devuelve {}. No inventes identificadores.\n" +
            candidates.joinToString("\n", transform = McpTool::describe)
        val answer = generate(listOf(ChatMessage("system", instruction), ChatMessage("user", query)), deterministic = true)
        return McpSelection.parse(answer, candidates)
    }

    internal fun review(call: Pending) { pending = call }
    internal fun dismiss(): Pending? = pending.also { pending = null }
    internal fun stop() { if (canStop) turnJob?.cancel() }

    internal suspend fun execute(call: Pending): String = try {
        call.client.call(call.selection.tool, call.selection.arguments)
    } catch (error: McpFailure) {
        throw error
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        throw McpFailure.Unavailable()
    }

    fun leave() {
        stop()
        dismiss()
    }

    fun close() {
        models.cancelDownload()
        turnJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch { engine.close() }
    }

    private suspend fun generate(
        messages: List<ChatMessage>, deterministic: Boolean = false, onText: suspend (String) -> Unit = {}
    ): String {
        val model = models.selected ?: throw UnsupportedOperationException()
        val file = models.file(model) ?: throw UnsupportedOperationException()
        return engine.generate(model, file, messages, deterministic) { withContext(Dispatchers.Main.immediate) { onText(it) } }
    }

    private suspend fun HubMcp.discoverOrFail() = try {
        discover()
    } catch (error: McpFailure) {
        throw error
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        throw McpFailure.Unavailable()
    }

    class Pending internal constructor(internal val client: HubMcp, internal val selection: McpSelection) {
        val title get() = selection.tool.title
        val arguments get() = selection.arguments.toString(2)
        val readOnly get() = selection.tool.readOnly
    }
}
