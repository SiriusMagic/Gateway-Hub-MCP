package com.zirakusa.gatewayhub.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64

/** What a package may allow (the provider maps each to the risk its tools declare). */
val ACTS = listOf("leer", "revisar", "escribir", "modificar", "revertir")

/** A package this Gateway issued: never its signature, only what the screens show and the status the app reported. */
data class IssuedPackage(val id: String, val app: String, val agent: String, val label: String, val acts: List<String>,
    val exp: Long, val status: String) {
    fun json(): JSONObject = JSONObject().put("id", id).put("app", app).put("agent", agent).put("label", label)
        .put("acts", JSONArray(acts)).put("exp", exp).put("status", status)
    companion object {
        fun parse(o: JSONObject) = IssuedPackage(o.getString("id"), o.getString("app"), o.getString("agent"), o.optString("label"),
            o.getJSONArray("acts").let { a -> (0 until a.length()).map { a.getString(it) } }, o.getLong("exp"), o.optString("status"))
    }
}

/** Packages issued and the instance each provider app answered with, kept between launches. */
interface PackageLedger {
    fun packages(): List<IssuedPackage>
    fun savePackages(list: List<IssuedPackage>)
    fun instance(app: String): String?
    fun saveInstance(app: String, instance: String)
}

class MemoryLedger : PackageLedger {
    private var list = listOf<IssuedPackage>(); private val instances = mutableMapOf<String, String>()
    override fun packages() = list
    override fun savePackages(list: List<IssuedPackage>) { this.list = list }
    override fun instance(app: String) = instances[app]
    override fun saveInstance(app: String, instance: String) { instances[app] = instance }
}

class PrefsLedger(context: Context) : PackageLedger {
    private val prefs = context.getSharedPreferences("hub_packages", Context.MODE_PRIVATE)
    override fun packages() = runCatching { JSONArray(prefs.getString("list", "[]")).let { a -> (0 until a.length()).map { IssuedPackage.parse(a.getJSONObject(it)) } } }.getOrDefault(emptyList())
    override fun savePackages(list: List<IssuedPackage>) = prefs.edit { putString("list", JSONArray(list.map { it.json() }).toString()) }
    override fun instance(app: String) = prefs.getString("instance.$app", null)
    override fun saveInstance(app: String, instance: String) = prefs.edit { putString("instance.$app", instance) }
}

/** Builds and signs one package (v1). The signed bytes travel as they are, so no JSON canonicalisation is needed. */
object PackageIssuer {
    fun issue(signer: PackageSigner, app: String, instance: String, agent: String, label: String, acts: List<String>,
        tools: List<String> = emptyList(), days: Int, now: Long = System.currentTimeMillis() / 1000): Pair<JSONObject, IssuedPackage> {
        val id = "pkg_" + ByteArray(12).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
        val exp = now + days.coerceIn(1, 365) * 86_400L
        val bytes = JSONObject().put("v", 1).put("id", id).put("env", signer.envId).put("aud", "$app#$instance").put("agent", agent)
            .put("label", label.take(80)).put("acts", JSONArray(acts.filter { it in ACTS })).put("tools", JSONArray(tools))
            .put("nbf", now - 60).put("exp", exp).toString().toByteArray()
        val enc = Base64.getUrlEncoder()
        val envelope = JSONObject().put("pkg", enc.encodeToString(bytes)).put("sig", enc.encodeToString(signer.sign(bytes)))
        return envelope to IssuedPackage(id, app, agent, label.take(80), acts.filter { it in ACTS }, exp, "offered")
    }
}
