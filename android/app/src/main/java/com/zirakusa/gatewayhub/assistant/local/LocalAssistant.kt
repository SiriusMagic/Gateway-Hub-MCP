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
    val models = LocalModels(app, scope, canChange = { !busy && pending == null }, onUnselected = { engine.close() })

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

    /**
     * What was said, as Lucía's own orders («llama a Luis», «qué hora es»): the model only rewrites, with her examples in
     * front of it; each order then goes through the rules, which still ask before changing anything. Empty: no order.
     */
    internal suspend fun orders(query: String, understood: (String) -> Boolean): List<String> {
        val examples = com.zirakusa.gatewayhub.assistant.Examples.closest(query)
        val instruction = "Eres el traductor de órdenes de Lucía, una asistente en un teléfono Android. Convierte lo que pide " +
            "la persona en una o varias órdenes cortas en español, copiando la forma de estos ejemplos y cambiando solo " +
            "nombres, números, horas, lugares o textos. Una orden por cada cosa que pide, en el mismo orden. " +
            "Devuelve solo JSON: {\"ordenes\":[\"...\"]}. Si no pide ninguna de estas acciones (por ejemplo, solo conversa), " +
            "devuelve {\"ordenes\":[]}. No inventes acciones que no estén aquí.\nEjemplos:\n" +
            examples.joinToString("\n") { "- $it" }
        val answer = generate(listOf(ChatMessage("system", instruction), ChatMessage("user", query)), deterministic = true,
            grammar = ToolGrammar.orders)
        return OrderReply.parse(answer, understood)
    }

    internal suspend fun decide(query: String): Pending? {
        val client = HubMcp(hub.link ?: throw McpFailure.Authorization())
        val selection = choose(query, client.discoverOrFail()) ?: return null
        return Pending(selection.tool.title, selection.arguments.toString(2), selection.tool.readOnly) {
            client.call(selection.tool, selection.arguments)
        }
    }

    /**
     * The «MCP» interpreter (layer 3): the model plans with the tools of the family apps on this phone (asked through each
     * app's hub link, no internet), in up to [Planner.MAX_STEPS] steps, seeing each answer. Null when none fits. What only
     * reads runs at once; a change waits for the card.
     */
    internal suspend fun mcpPlan(query: String): Pending? {
        val tools = phoneMcp.allTools()
        if (tools.isEmpty()) return null
        // One name per app and tool (Stone and Stone Video share tool names): «video.player_state».
        val named = tools.associateBy { t -> "${t.key}.${t.tool.name}" }
        val listed = named.map { (name, t) -> t.tool.copy(name = name, title = "${t.app.label}: ${t.tool.title}") }
        val today = java.time.LocalDate.now()
        fun say(s: McpSelection, r: org.json.JSONObject) =
            com.zirakusa.gatewayhub.assistant.Speech.say(named.getValue(s.tool.name).tool.name, r, today, s.arguments)
        val outcome = Planner({ done -> choose(query, listed, done) }, { s -> phoneMcp.callFamily(named.getValue(s.tool.name), s.arguments) },
            { s -> named[s.tool.name]?.tool?.readOnly == true }).plan()
        return when (outcome) {
            Planner.Outcome.Nothing -> null
            is Planner.Outcome.Read -> outcome.steps.last().let { (s, r) -> Pending(s.tool.title, s.arguments.toString(2), true) { say(s, r) } }
            is Planner.Outcome.Confirm -> outcome.change.let { s ->
                Pending(s.tool.title, s.arguments.toString(2), false) { say(s, phoneMcp.callFamily(named.getValue(s.tool.name), s.arguments)) }
            }
        }
    }

    internal suspend fun askStone(query: String): String? {
        val app = phoneMcp.stone()
        if (app.status != PhoneMcpStatus.READY) throw PhoneMcpError(app.status)
        val selection = choose(query, app.tools) ?: return null
        val answer = StoneRequests.answer(selection.tool.name, phoneMcp.call(app, selection), this.app.resources)
        history = (history + ChatMessage("user", query) + ChatMessage("assistant", answer)).takeLast(8)
        return answer
    }

    private suspend fun choose(query: String, tools: List<McpTool>, done: List<String> = emptyList()): McpSelection? {
        val candidates = McpSelection.candidates(query, tools)
        if (candidates.isEmpty()) return null
        val instruction = "Selecciona una herramienta que coincida con la petición en español. " +
            "Devuelve exclusivamente JSON con las claves tool (nombre exacto) y arguments (objeto). " +
            "Si ninguna herramienta coincide o faltan datos obligatorios, devuelve {}. No inventes identificadores.\n" +
            (if (done.isEmpty()) "" else "Ya hiciste estos pasos (herramienta, argumentos → respuesta). Usa sus identificadores " +
                "para el paso siguiente; si la petición ya está resuelta, devuelve {}:\n" + done.joinToString("\n") + "\n") +
            candidates.joinToString("\n", transform = McpTool::describe)
        val answer = generate(listOf(ChatMessage("system", instruction), ChatMessage("user", query)), deterministic = true,
            grammar = ToolGrammar.forTools(candidates.map { it.name }))
        return McpSelection.parse(answer, candidates)
    }

    internal fun review(call: Pending) { pending = call }
    internal fun dismiss(): Pending? = pending.also { pending = null }
    internal fun stop() { if (canStop) turnJob?.cancel() }

    internal suspend fun execute(call: Pending): String = try {
        call.run()
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
        models.downloads.cancel()
        turnJob?.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            try { engine.close() } finally { models.closeDocuments() }
        }
    }

    private suspend fun generate(
        messages: List<ChatMessage>, deterministic: Boolean = false, grammar: String? = null, onText: suspend (String) -> Unit = {}
    ): String {
        val (template, file) = models.current() ?: throw UnsupportedOperationException()
        return engine.generate(template, file, messages, deterministic, grammar) { withContext(Dispatchers.Main.immediate) { onText(it) } }
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

    /** A tool call chosen by the model: shown on a card (title, arguments) before it runs, unless it only reads. */
    class Pending internal constructor(val title: String, val arguments: String, val readOnly: Boolean, internal val run: suspend () -> String)
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
