package com.zirakusa.gatewayhub.data

import android.content.Context
import androidx.core.net.toUri

/** What an app answered when asked for its link: the link, or why not (its own words; null for our own codes). */
data class LinkAnswer(val link: String?, val error: String?, val message: String?)

fun interface AppLinker { fun requestLink(app: LocalApp): LinkAnswer }

/** Tells an app of the same developer the chosen mode ("safe" or "background"); true when it applied it. */
fun interface ModeSender { fun sendMode(app: LocalApp, mode: String): Boolean }

/**
 * Asks an app on this phone for the link reserved for the hub (Redirección hub). Only apps signed by
 * the same developer are asked; Android also enforces it with a signature permission, and the app
 * itself checks our signature and its own switch before answering.
 */
class ProviderLinker(private val context: Context) : AppLinker, ModeSender {
    /** Stone 1.7.14+: call("mode", "safe"|"background") on the same signature-protected channel. Older apps ignore it. */
    override fun sendMode(app: LocalApp, mode: String): Boolean {
        val authority = app.linkAuthority ?: return false
        if (!app.sameDeveloper) return false
        return runCatching { context.contentResolver.call("content://$authority".toUri(), "mode", mode, null)?.getString("mode") == mode }
            .getOrDefault(false)
    }

    override fun requestLink(app: LocalApp): LinkAnswer {
        val authority = app.linkAuthority
        if (!app.sameDeveloper || authority == null) return LinkAnswer(null, "NOT_SUPPORTED", null)
        return try {
            val answer = context.contentResolver.call("content://$authority".toUri(), "link", null, null)
                ?: return LinkAnswer(null, "NO_ANSWER", null)
            LinkAnswer(answer.getString("link"), answer.getString("error"), answer.getString("message"))
        } catch (e: SecurityException) {
            LinkAnswer(null, "NO_PERMISSION", null)
        } catch (e: Exception) {
            LinkAnswer(null, "FAILED", null)
        }
    }
}
