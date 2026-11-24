package com.zirakusa.gatewayhub.assistant

import android.Manifest
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * The assistant's hands on Android. It speaks to the family apps with Android's standard orders (the same any assistant
 * uses: «poner alarma», «reproducir», «marcar») plus the family's own (FAMILIA.md §10), and falls back to opening the
 * app when an older version doesn't know the order yet. Calls and messages only open the screen ready to confirm.
 */
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

    private val stone get() = Catalog.byId("stone")

    /** Android's «play from search»: an empty query means «anything», which Stone plays shuffled. */
    /** Called when music was asked for but nothing sounds a few seconds later (see [PlayCheck]). */
    var onSilent: () -> Unit = {}
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val check = PlayCheck(playing = { audio?.isMusicActive == true }, press = { button(KeyEvent.KEYCODE_MEDIA_PLAY) },
        later = { ms, run -> main.postDelayed(run, ms) }, silent = { onSilent() })

    override fun playMusic(query: String?): Boolean {
        val play = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).putExtra(SearchManager.QUERY, query.orEmpty())
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, if (query == null) "vnd.android.cursor.item/*" else MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
        // An older Stone does not know the order: it is opened, and PlayCheck presses «play» once it is up.
        val ok = start(play, stone, fallback = false) || open(stone)
        if (ok) check.start()
        return ok
    }

    override fun playRecommended(): Boolean {
        if (!start(Intent(PLAY_RECOMMENDED), stone, fallback = false)) return playMusic(null)
        check.start(); return true
    }

    /** A media button, for whatever player has the music. */
    private fun button(code: Int): Boolean {
        val a = audio ?: return false
        a.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)); a.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return true
    }

    override fun media(key: MediaKey): Boolean {
        val code = when (key) { MediaKey.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE; MediaKey.RESUME -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaKey.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT; MediaKey.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            // Players go back to the start of the song on «previous» once a few seconds have played.
            MediaKey.RESTART -> KeyEvent.KEYCODE_MEDIA_PREVIOUS }
        if (!button(code)) return false
        if (key == MediaKey.PAUSE) check.cancel() else check.start()
        return true
    }

    override fun alarm(hour: Int, minute: Int) = start(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, hour)
        .putExtra(AlarmClock.EXTRA_MINUTES, minute).putExtra(AlarmClock.EXTRA_SKIP_UI, true), Catalog.byId("reloj"))

    override fun timer(seconds: Int) = start(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, seconds)
        .putExtra(AlarmClock.EXTRA_SKIP_UI, true), Catalog.byId("reloj"))

    override fun dial(number: String) = start(Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(number)}".toUri()), Catalog.byId("telefono"))

    override fun message(number: String, text: String?) = start(Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(number)}".toUri())
        .apply { if (text != null) putExtra("sms_body", text) }, Catalog.byId("sms"))

    /**
     * The contact said: from the phone's contacts with the permission, otherwise through Teléfono (it already has it),
     * so Lucía needs no permission of her own. Close names are tried too («Vox» finds «Box»).
     */
    override fun person(name: String): Person? {
        val allowed = ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val found = if (allowed) contacts(name).ifEmpty { People.guesses(name).flatMap(::contacts).distinct() }
            else byPhoneApp(name) ?: throw Hands.NoContacts()
        return People.best(found, name)
    }

    private fun contacts(query: String): List<Person> {
        val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(query))
        val found = ArrayList<Person>()
        runCatching {
            app.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { c ->
                while (c.moveToNext() && found.size < 50) found += Person(c.getString(0).orEmpty(), c.getString(1).orEmpty())
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
        val request = org.json.JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call")
            .put("params", org.json.JSONObject().put("name", name).put("arguments", args)).toString()
        val answer = try { this.app.contentResolver.call("content://$p.hublink".toUri(), "tool", null, Bundle().apply { putString("request", request) }) }
            catch (_: SecurityException) { throw Hands.ToolError("NO_PERMISSION", "Reinstala Gateway Hub después de la app.") }
            catch (_: IllegalArgumentException) { return null } ?: return null
        when (answer.getString("error")) {
            null -> Unit
            "REDIRECT_OFF" -> throw Hands.ToolOff()
            else -> throw Hands.ToolError(answer.getString("error").orEmpty(), answer.getString("message").orEmpty())
        }
        val result = org.json.JSONObject(answer.getString("response") ?: return null).optJSONObject("result") ?: return null
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
        const val PLAY_RECOMMENDED = "com.zirakusa.family.action.PLAY_RECOMMENDED"
        const val CAPTURE = "com.zirakusa.family.action.CAPTURE"
    }
}
