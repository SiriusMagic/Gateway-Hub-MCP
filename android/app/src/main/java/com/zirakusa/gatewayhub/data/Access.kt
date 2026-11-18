package com.zirakusa.gatewayhub.data

/** Hub address + owner token read from one paste. */
data class Access(val url: String, val token: String)

/**
 * Understands what people actually paste: "https://hub#hub_…", "https://hub/admin?token=hub_…",
 * the address and the token on two lines, or only the token (then [defaultUrl] is used).
 * A Stone link or token is recognised and explained, because it opens Stone, not the hub.
 */
object AccessParser {
    private val URL = Regex("""https?://[^\s#?]+(?:\?[^\s#]*)?(?:#\S*)?""")
    private val OWNER = Regex("""\bhub_[A-Za-z0-9_-]{20,}""")
    private val AGENT = Regex("""\bgw_[A-Za-z0-9_-]{20,}""")
    private val STONE = Regex("""\bstn_[A-Za-z0-9_-]{10,}|/d/[a-z0-9]{12,40}/mcp""")

    fun parse(text: String, defaultUrl: String): Access {
        val pasted = text.trim()
        if (pasted.isEmpty()) throw HubException("EMPTY", "Pega el acceso del hub (dirección + token) o solo el token.")
        if (STONE.containsMatchIn(pasted)) throw HubException("STONE_LINK",
            "Eso es un enlace o token de Stone, no del hub. Entra con el token del propietario (empieza por hub_) " +
                "y luego pega el enlace de Stone en Escáner o Apps › Vincular Stone.")
        if (AGENT.containsMatchIn(pasted) && !OWNER.containsMatchIn(pasted)) throw HubException("AGENT_LINK",
            "Eso es el enlace de un agente (gw_…): sirve para Claude o ChatGPT, no para administrar. Usa el token del propietario (hub_…).")
        val url = URL.find(pasted)?.value?.substringBefore('#')?.substringBefore('?')
        val token = OWNER.find(pasted)?.value
            ?: pasted.replace(URL, " ").split(Regex("\\s+")).firstOrNull { it.length >= 20 }
            ?: throw HubException("NO_TOKEN", "No encuentro el token del propietario (empieza por hub_).")
        return Access(HubApi.normalize(url ?: defaultUrl), token)
    }

    /** The one line to keep in a password manager and paste here next time. */
    fun format(access: Access) = "${access.url}#${access.token}"
}
