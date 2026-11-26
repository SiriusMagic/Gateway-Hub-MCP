package com.zirakusa.gatewayhub.assistant

import android.content.Context
import androidx.core.content.edit
import com.zirakusa.gatewayhub.data.HttpTransport
import com.zirakusa.gatewayhub.data.HubApi
import com.zirakusa.gatewayhub.data.HubTransport
import com.zirakusa.gatewayhub.data.RecordCipher
import com.zirakusa.gatewayhub.data.SessionStore
import com.zirakusa.gatewayhub.data.KeystoreSessionStore
import com.zirakusa.gatewayhub.data.PrivateValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

internal enum class IntentVote(val wire: String) { LIKE("funcionó"), DISLIKE("falló"), CORRECTED("corregido") }

/** Android-private evidence and an outbox, independently of an active Hub connection. */
internal class IntentFeedback(
    context: Context,
    scope: CoroutineScope,
    private val crypto: RecordCipher = PrivateValues("arka_intent_feedback"),
    private val sessions: SessionStore = KeystoreSessionStore(context),
    private val transport: HubTransport = HttpTransport
) {
    private val prefs = context.getSharedPreferences("intent_feedback", Context.MODE_PRIVATE)
    private val work = Channel<Unit>(Channel.CONFLATED)
    private val lock = Any()
    private val phoneKey = "installation"

    init {
        scope.launch(Dispatchers.IO) {
            for (ignored in work) runCatching { flush() }
        }
    }

    val sharing get() = prefs.getBoolean("sharing", false)

    fun share(value: Boolean) {
        val host = sessions.load()?.first?.let { runCatching { HubApi.normalize(it) }.getOrNull() }
        prefs.edit { putBoolean("sharing", value); putString("sharing_host", host.takeIf { value }) }
        if (value) retry()
    }

    fun retry() { if (sharing) work.trySend(Unit) }
    fun save(turn: Turn, rendered: String, vote: IntentVote? = null, wrong: String = "", expected: String = ""): Boolean =
        synchronized(lock) {
            val evidence = turn.evidence ?: return false
            val key = "turn/${evidence.id}"
            runCatching {
                val before = read(key)
                val record = encode(evidence, turn, rendered, vote, wrong, expected)
                if (vote == null && before?.has("status") == true) {
                    for (field in listOf("status", "wrong", "expected", "sent")) record.put(field, before.opt(field))
                    record.put("id", before.optString("id"))
                }
                write(key, record)
                if (vote != null) retry()
            }.isSuccess
        }

    fun alias(name: String): String? = synchronized(lock) { read(aliasKey(name))?.optString("uri") }

    fun putAlias(name: String, uri: String?) = synchronized(lock) {
        if (uri == null) prefs.edit { remove(aliasKey(name)) }
        else write(aliasKey(name), JSONObject().put("name", name).put("uri", uri))
    }

    fun aliases(): List<String> = synchronized(lock) {
        prefs.all.keys.filter { it.startsWith("alias/") }.mapNotNull { read(it)?.optString("name") }.sorted()
    }

    private fun aliasKey(name: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(Words.normalize(name).toByteArray())
        return "alias/" + android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }

    private fun encode(e: IntentEvidence, turn: Turn, rendered: String, vote: IntentVote?, wrong: String, expected: String) =
        JSONObject().put("id", UUID.randomUUID().toString())
            .put("actionId", e.id).put("step", e.step).put("original", e.plan.original.take(1000))
            .put("version", IntentPlan.VERSION).put("model", e.plan.source?.let {
                JSONObject().put("id", it.model).put("templateVersion", it.template)
            })
            .put("plan", JSONArray(e.plan.steps.map { step ->
                JSONObject().put("phrase", step.phrase.take(400)).put("action", step.command.action.name)
                    .put("app", step.app?.id).put("parameters", JSONObject(step.command.slots))
                    .put("dependencies", JSONArray(step.dependencies))
            }))
            .put("result", JSONObject().put("ok", turn.ok).put("code", turn.reply.say.name)
                .put("text", rendered.take(1600)).put("ms", turn.millis).put("observed", JSONObject(turn.observed)))
            .put("status", vote?.wire).put("wrong", wrong.take(800)).put("expected", expected.take(800))
            .put("at", System.currentTimeMillis())

    private fun read(key: String): JSONObject? = runCatching {
        prefs.getString(key, null)?.let { JSONObject(crypto.open(it)) }
    }.getOrNull()

    private fun write(key: String, value: JSONObject) {
        prefs.edit { putString(key, crypto.seal(value.toString())) }
    }

    internal fun flush() {
        if (!sharing) return
        val (url, owner) = sessions.load() ?: return
        val base = HubApi.normalize(url)
        val sharingHost = prefs.getString("sharing_host", null)
        if (sharingHost != null && sharingHost != base) return
        if (sharingHost == null && sharing) prefs.edit { putString("sharing_host", base) }
        val pending = synchronized(lock) {
            prefs.all.keys.asSequence().filter { it.startsWith("turn/") }.mapNotNull { key ->
                val record = read(key) ?: return@mapNotNull null
                if (record.has("status") && !record.optBoolean("sent")) key to record else null
            }.sortedBy { it.second.optLong("at") }.take(30).toList()
        }
        if (pending.isEmpty()) return
        val installation = prefs.getString(phoneKey, null) ?: UUID.randomUUID().toString().also {
            prefs.edit { putString(phoneKey, it) }
        }
        val issued = synchronized(lock) { read("credential") }?.takeIf { it.optString("host") == base }
        val token = issued?.optString("token") ?: run {
            if (!sharing) return
            val (status, response) = transport.send("POST", "$base/api/feedback/credentials", owner,
                JSONObject().put("installation", installation).toString())
            if (status !in 200..299) return
            JSONObject(response).getString("token").also { next ->
                synchronized(lock) { write("credential", JSONObject().put("host", base).put("token", next)) }
            }
        }
        for ((key, record) in pending) {
            if (!sharing || sessions.load() != (url to owner)) return
            val (status, _) = transport.send("POST", "$base/api/feedback/events", token, record.toString())
            if (status == 401) {
                synchronized(lock) { prefs.edit { remove("credential") } }
                if (issued != null) retry()
                return
            }
            if (status !in 200..299) return
            synchronized(lock) {
                read(key)?.takeIf { it.optString("id") == record.optString("id") }?.let {
                    write(key, it.put("sent", true))
                }
            }
        }
        if (pending.size == 30) retry()
    }
}
