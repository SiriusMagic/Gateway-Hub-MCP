package com.zirakusa.gatewayhub.data

import android.content.Context
import androidx.core.net.toUri

/** What an app answered when asked for its link: the link, or why not (its own words; null for our own codes). */
data class LinkAnswer(val link: String?, val error: String?, val message: String?)

fun interface AppLinker { fun requestLink(app: LocalApp): LinkAnswer }

/**
 * Asks an app on this phone for the link reserved for the hub (Redirección hub). Only apps signed by
 * the same developer are asked; Android also enforces it with a signature permission, and the app
 * itself checks our signature and its own switch before answering.
 */
class ProviderLinker(private val context: Context) : AppLinker {
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
