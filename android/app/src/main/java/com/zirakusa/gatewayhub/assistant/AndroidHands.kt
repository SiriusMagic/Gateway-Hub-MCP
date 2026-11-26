package com.zirakusa.gatewayhub.assistant

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/** Android integrations for the signed family MCP provider and system actions still awaiting migration. */
class AndroidHands(context: Context) : Hands {
    private val app = context.applicationContext
    private val pm = app.packageManager

    private fun pkg(a: FamilyApp) = a.packages.firstOrNull { pm.getLaunchIntentForPackage(it) != null }
    override fun installed(app: FamilyApp) = pkg(app) != null

    /** Starts [intent] in [a] when it understands it; otherwise (when [fallback]) the same order for any app. */
    private fun start(intent: Intent, a: FamilyApp?, fallback: Boolean = true): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val mine = a?.let(::pkg)?.let { Intent(intent).setPackage(it) }
        for (i in listOfNotNull(mine?.takeIf { it.resolveActivity(pm) != null }, intent.takeIf { fallback && it.resolveActivity(pm) != null })) {
            try { app.startActivity(i); return true } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
        }
        return false
    }

    override fun open(app: FamilyApp): Boolean {
        // A screen inside a package (Contactos in Teléfono) first; otherwise the first installed package's icon.
        val inside = app.activity?.let { a -> app.packages.map { Intent().setClassName(it, a) }.firstOrNull { it.resolveActivity(pm) != null } }
        val launch = inside ?: pkg(app)?.let(pm::getLaunchIntentForPackage) ?: return false
        return try { this.app.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (_: ActivityNotFoundException) { false }
    }

    override fun timer(seconds: Int) = start(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, seconds)
        .putExtra(AlarmClock.EXTRA_SKIP_UI, true), Catalog.byId("reloj"))

    override fun dial(number: String) = start(Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(number)}".toUri()), Catalog.byId("telefono"))

    override fun placeCall(number: String, speaker: Boolean): Boolean {
        if (!number.matches(Regex("[+0-9 ().-]+"))) return false
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            throw Hands.NoCallPermission()
        val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null))
            .putExtra(android.telecom.TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, speaker)
        return launchExternal(intent)
    }

    override fun searchWeb(query: String): Boolean {
        val url = Uri.Builder().scheme("https").authority("www.google.com").path("search")
            .appendQueryParameter("q", query).build()
        return launchExternal(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE))
    }

    private fun launchExternal(intent: Intent): Boolean = try {
        app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    override fun message(number: String, text: String?) = start(Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(number)}".toUri())
        .apply { if (text != null) putExtra("sms_body", text) }, Catalog.byId("sms"))

    /**
     * The contact said: from the phone's contacts with the permission, otherwise through Teléfono (it already has it),
     * so Lucía needs no permission of her own. Close names are tried too («Vox» finds «Box»).
     */
    var contactAlias: (String) -> String? = { null }

    override fun person(name: String): Person? {
        val allowed = ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        contactAlias(name)?.let {
            if (!allowed) throw Hands.NoContacts()
            return ContactAliases.resolve(app, it.toUri())
        }
        val found = if (allowed) contacts(name).ifEmpty { People.guesses(name).flatMap(::contacts).distinct() }
            else byPhoneApp(name) ?: throw Hands.NoContacts()
        val choices = People.candidates(found, name)
        if (choices.size > 1) throw Hands.AmbiguousContact()
        return choices.singleOrNull()
    }

    private fun contacts(query: String): List<Person> {
        val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(query))
        val found = ArrayList<Person>()
        runCatching {
            app.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY), null, null, null)?.use { c ->
                while (c.moveToNext() && found.size < 50) found += Person(c.getString(0).orEmpty(), c.getString(1).orEmpty(), c.getString(2))
            }
        }
        return found
    }

    /** Teléfono's contacts_search; null when Teléfono is missing, too old or can't read the contacts either. */
    private fun byPhoneApp(name: String): List<Person>? {
        fun search(q: String): List<Person>? = runCatching {
            tool(Catalog.byId("telefono"), "contacts_search", org.json.JSONObject().put("query", q.take(100)).put("limit", 50))
        }.getOrNull()?.optJSONArray("contacts")?.let { list ->
            (0 until list.length()).mapNotNull { list.optJSONObject(it) }.map { Person(it.optString("name"), it.optString("phone").takeIf { p -> p != "null" }.orEmpty()) }
        }
        val first = search(name) ?: return null
        return first.ifEmpty { People.guesses(name).flatMap { search(it).orEmpty() }.distinct() }
    }

    /** The «Hoy» summary each family app gives (only to apps signed by the same developer, like this one). */
    override fun summary(app: FamilyApp): Summary? {
        val p = pkg(app) ?: return null
        return runCatching {
            val b = this.app.contentResolver.call("content://$p.summary".toUri(), "summary", null, null) ?: return null
            Summary(b.getString("title").orEmpty(), b.getStringArrayList("lines").orEmpty(), b.getString("empty").orEmpty())
        }.getOrNull()
    }

    /** Notas' inbox: only adds a note, and only for apps signed by the same developer (the ACT permission). */
    override fun saveNote(title: String, body: String, folder: String): Long? {
        val p = pkg(Catalog.byId("notas")) ?: return null
        val extras = Bundle().apply { putString("title", title); putString("body", body); putString("folder", folder) }
        return runCatching { app.contentResolver.call("content://$p.inbox".toUri(), "add_note", null, extras)?.getLong("id") }
            .getOrNull()?.takeIf { it > 0 }
    }

    /**
     * Back to Lucía once the other app has started (music keeps playing behind). Android lets the Hub come to the front
     * while it may draw over other apps or is the phone's assistant; otherwise the other app simply stays.
     */
    override fun comeBack() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching {
                app.startActivity(Intent(app, AssistantActivity::class.java).setAction(AssistantActivity.RETURN_TO_ASSISTANT)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
        }, 1_800)
    }

    override fun inCall(): Boolean =
        audio?.mode in setOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)

    private fun phoneAllowed() = ContextCompat.checkSelfPermission(app, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
    private val telecom get() = app.getSystemService(android.telecom.TelecomManager::class.java)

    @Suppress("DEPRECATION") // the way for an assistant (not the phone app) to answer; still supported
    override fun answerCall(): Boolean {
        if (!phoneAllowed()) throw Hands.NoPhone()
        val t = telecom ?: return false
        // Nothing happens when no call is ringing (knowing it would need another permission).
        return try { t.acceptRingingCall(); true } catch (_: SecurityException) { throw Hands.NoPhone() }
    }

    @Suppress("DEPRECATION")
    override fun endCall(): Boolean {
        if (!phoneAllowed()) throw Hands.NoPhone()
        val t = telecom ?: return false
        return try { if (android.os.Build.VERSION.SDK_INT >= 28) t.endCall() else false } catch (_: SecurityException) { throw Hands.NoPhone() }
    }

    /**
     * One MCP tool of a family app, on the phone: the app's link channel (signature-protected) runs it with the hub's own
     * token, through the same handler as the relay, so all its rules hold. No internet.
     */
    override fun tool(app: FamilyApp, name: String, args: org.json.JSONObject): org.json.JSONObject? {
        val p = pkg(app) ?: return null
        if (!Prefs(this.app).localMcp) throw Hands.ToolOff()
        val request = org.json.JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call")
            .put("params", org.json.JSONObject().put("name", name).put("arguments", args)).toString()
        val answer = try { this.app.contentResolver.call("content://$p.hublink".toUri(), "tool", null, Bundle().apply { putString("request", request) }) }
            catch (_: SecurityException) { throw Hands.ToolError("NO_PERMISSION", "Reinstala Arka después de la app.") }
            catch (_: IllegalArgumentException) { return null } ?: return null
        when (answer.getString("error")) {
            null -> Unit
            "REDIRECT_OFF" -> throw Hands.ToolOff()
            else -> throw Hands.ToolError(answer.getString("error").orEmpty(), answer.getString("message").orEmpty())
        }
        val rpc = org.json.JSONObject(answer.getString("response") ?: return null)
        rpc.optJSONObject("error")?.let {
            val message = it.optString("message")
            val code = if (it.optInt("code") == -32602 && message.startsWith("Unknown tool")) "UNKNOWN_TOOL" else "MCP_RPC_ERROR"
            throw Hands.ToolError(code, message)
        }
        val result = rpc.optJSONObject("result") ?: return null
        val content = result.optJSONObject("structuredContent") ?: org.json.JSONObject()
        if (result.optBoolean("isError")) content.optJSONObject("error").let { e ->
            throw Hands.ToolError(e?.optString("code").orEmpty(), e?.optString("message").orEmpty())
        }
        return content
    }

    override fun capture(selfie: Boolean) = start(Intent(CAPTURE).putExtra("selfie", selfie), Catalog.byId("camara"), fallback = false)

    /** The torch of the back camera; no permission needed. */
    override fun torch(on: Boolean): Boolean = runCatching {
        val cam = app.getSystemService(android.hardware.camera2.CameraManager::class.java) ?: return false
        val id = cam.cameraIdList.firstOrNull { cam.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return false
        cam.setTorchMode(id, on); true
    }.getOrDefault(false)

    private val audio get() = app.getSystemService(AudioManager::class.java)
    override fun volume(): Int = audio?.let { a -> 100 * a.getStreamVolume(AudioManager.STREAM_MUSIC) / maxOf(1, a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) } ?: 0
    /** The media volume, shown on screen like the phone's buttons do; false when the phone doesn't allow it (Do not disturb). */
    override fun setVolume(percent: Int): Boolean = runCatching {
        val a = audio ?: return false
        val max = a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        a.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(max * percent.coerceIn(0, 100) / 100f), AudioManager.FLAG_SHOW_UI)
        true
    }.getOrDefault(false)

    /** Opens a note in Notas (after a count was saved there). */
    fun openNote(id: Long): Boolean {
        val p = pkg(Catalog.byId("notas")) ?: return false
        val i = (pm.getLaunchIntentForPackage(p) ?: return false).putExtra("note_id", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { app.startActivity(i); true } catch (_: ActivityNotFoundException) { false }
    }

    companion object {
        const val CAPTURE = "com.zirakusa.family.action.CAPTURE"
    }
}
