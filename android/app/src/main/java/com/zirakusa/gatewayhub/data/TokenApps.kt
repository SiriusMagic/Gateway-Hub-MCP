package com.zirakusa.gatewayhub.data

/** Which app a pasted link or token belongs to: its hub prefix and its name. */
data class TokenApp(val prefix: String, val name: String, val supported: Boolean = true)

/**
 * Each app starts its tokens with its own mark (stn_ Stone, lum_ Luma, cal_ Calculadora, fil_ Filem, not_ Notas),
 * so the pasted link already says which app it is: no need to pick the app by hand.
 */
object TokenApps {
    private val apps = listOf(
        "stn_" to TokenApp("music_", "Stone"), "lum_" to TokenApp("gallery_", "Luma"),
        "cal_" to TokenApp("calc_", "Calculadora"), "fil_" to TokenApp("files_", "Filem"),
        // The hub still has its own notes_ (the Notas app cannot take that name yet).
        "not_" to TokenApp("notes_", "Notas", supported = false),
    )
    private val TOKEN = Regex("""(?:[?&]token=|\b)([a-z]{3}_)[A-Za-z0-9_-]{20,}""")

    /** The app of [text], or null when it carries no known app token (hub_ and gw_ are not app tokens). */
    fun identify(text: String): TokenApp? {
        val mark = TOKEN.find(text.trim())?.groupValues?.get(1) ?: return null
        return apps.firstOrNull { it.first == mark }?.second
    }
}
