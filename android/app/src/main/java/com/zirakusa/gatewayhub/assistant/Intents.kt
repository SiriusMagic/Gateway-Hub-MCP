package com.zirakusa.gatewayhub.assistant

/**
 * One sentence, several orders: «Hey Luna, llama a Luis por favor y ayúdame con la hora» is «llama a Luis» and «qué hora
 * es». Courtesy and the wake words go; each piece must be an order the rules know, or the sentence stays whole (so «a las
 * 7 y media» is never cut). Pure; no model needed.
 */
object Intents {
    private val wake = Regex("^(?:(?:hey|oye|ey|hola|ok)\\s+)?(?:luna|lucia|lucía)\\b[,.:!]*\\s*", RegexOption.IGNORE_CASE)
    private val joints = Regex("\\s*(?:[,;]|\\s(?:y luego|y despues|y después|y tambien|y también|luego|despues|después|tambien|también|y|e)\\s)\\s*", RegexOption.IGNORE_CASE)
    private val asks = Regex("^(?:(?:por favor|porfa|porfavor)\\s+)?(?:ayudame|ayúdame|me ayudas|me puedes ayudar|necesito ayuda|dime|me dices|quisiera saber|quiero saber|podrias decirme|podrías decirme|recuerdame decirte)(?:\\s+(?:con|a|sobre))?\\s+", RegexOption.IGNORE_CASE)
    private val thanks = Regex("[,.!¡¿?\\s]*(?:por favor|porfa|gracias|muchas gracias)[.!]*$", RegexOption.IGNORE_CASE)
    /** Short subjects that mean a question («la hora» → «qué hora es»). */
    private val subjects = mapOf("hora" to "qué hora es", "la hora" to "qué hora es", "fecha" to "qué día es", "la fecha" to "qué día es",
        "el dia" to "qué día es", "el clima" to "qué tiempo hace", "clima" to "qué tiempo hace", "el tiempo" to "qué tiempo hace",
        "mi agenda" to "qué tengo hoy", "la agenda" to "qué tengo hoy")

    /** «Sí» to a question Lucía asked. */
    fun yes(text: String) = Regex("^(?:si|claro|confirmo|hazlo|dale|adelante|ok|okey|de acuerdo|vale|por supuesto)\\b")
        .containsMatchIn(Words.normalize(text).replace('ñ', 'n'))

    /** What was said without «Hey Luna», «por favor», «gracias» and «ayúdame con»; a bare subject becomes its question. */
    fun clean(text: String): String {
        var t = text.trim().replace(wake, "").replace(thanks, "").trim().trimEnd('.', '!', ',')
        repeat(2) { t = t.replace(asks, "").replace(thanks, "").trim() }
        return subjects[Words.normalize(t)] ?: t
    }

    /**
     * The orders in [text], in the order said: two or more when every piece is an order [understood]; otherwise the whole
     * (cleaned) sentence alone.
     */
    fun split(text: String, understood: (String) -> Boolean): List<String> {
        val whole = clean(text)
        val parts = whole.split(joints).map(::clean).filter { it.isNotBlank() }
        return if (parts.size >= 2 && parts.all(understood)) parts else listOf(whole)
    }
}
