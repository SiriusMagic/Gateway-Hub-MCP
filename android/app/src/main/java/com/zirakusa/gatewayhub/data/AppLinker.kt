package com.zirakusa.gatewayhub.data

import android.content.Context
import androidx.core.net.toUri

/** What an app answered when asked for its link: the link, or why not (its own words). */
data class LinkAnswer(val link: String?, val error: String?, val message: String?)

fun interface AppLinker {
    fun requestLink(app: LocalApp): LinkAnswer
    /** «Conectar todas»: the app turns «Redireccionar a Hub» on itself (the user's tap here is the consent), then answers like [requestLink]. */
    fun connect(app: LocalApp): LinkAnswer = requestLink(app)
}

/**
 * Asks an app on this phone for the link reserved for the hub (Redireccionar a Hub). Only apps signed by
 * the same developer are asked; Android also enforces it with a signature permission, and the app
 * itself checks our signature and its own switch before answering.
 */
class ProviderLinker(private val context: Context) : AppLinker {
    override fun requestLink(app: LocalApp): LinkAnswer = ask(app, "link")
    override fun connect(app: LocalApp): LinkAnswer = ask(app, "connect").let { a ->
        // An app that predates «connect» answers nothing: ask it the old way.
        if (a.error == "NO_ANSWER") ask(app, "link") else a
    }

    private fun ask(app: LocalApp, method: String): LinkAnswer {
        val authority = app.linkAuthority
        if (!app.sameDeveloper || authority == null) return LinkAnswer(null, "NOT_SUPPORTED", "Esta app no permite vincularse sola.")
        return try {
            val answer = context.contentResolver.call("content://$authority".toUri(), method, null, null)
                ?: return LinkAnswer(null, "NO_ANSWER", "La app no respondió.")
            LinkAnswer(answer.getString("link"), answer.getString("error"), answer.getString("message"))
        } catch (e: SecurityException) {
            LinkAnswer(null, "NO_PERMISSION", "Android no dio permiso: reinstala Gateway Hub después de la app.")
        } catch (e: Exception) {
            LinkAnswer(null, "FAILED", "No se pudo hablar con la app.")
        }
    }
}
