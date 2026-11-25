package com.zirakusa.gatewayhub.assistant

/**
 * One sentence, several orders: «Hey Luna, llama a Luis por favor y ayúdame con la hora» is «llama a Luis» and «qué hora
 * es». Courtesy and the wake words go; each piece must be an order the rules know, or the sentence stays whole (so «a las
 * 7 y media» is never cut). Pure; no model needed.
 */
object Intents {
    private val wake = Regex("^(?:(?:hey|oye|ey|hola|ok)\\s+)?(?:luna|lucia|lucía)\\b[,.:!]*\\s*", RegexOption.IGNORE_CASE)
    private val joints = Regex("""\s*(?:;|(?<!\d),|,(?!\d)|\s(?:y luego|y despues|y después|y tambien|y también|luego|despues|después|tambien|también|y(?!\s+(?:m[aá]s|menos|media|medio|cuarto|un[oa]?|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|\d+)\b)|e)\s)\s*""", RegexOption.IGNORE_CASE)
    private val nextAction = Regex("""\s+(?=(?:(?:necesito|quiero) que )?(?:calcul(?:a|ar|es)|llama(?:le|r)?|sube|baja|reproduce|pon)\b)""", RegexOption.IGNORE_CASE)
    private val asksForAction = Regex("""^(?:necesito|quiero) que\s+(calcules|llames|subas|bajes|reproduzcas|pongas)\b""", RegexOption.IGNORE_CASE)
    private val imperatives = mapOf("calcules" to "calcula", "llames" to "llama", "subas" to "sube",
        "bajes" to "baja", "reproduzcas" to "reproduce", "pongas" to "pon")
    private val closedActions = setOf(Action.CALCULATE, Action.VOLUME)
    private val asks = Regex("^(?:(?:por favor|porfa|porfavor)\\s+)?(?:ayudame|ayúdame|me ayudas|me puedes ayudar|necesito ayuda|dime|me dices|quisiera saber|quiero saber|podrias decirme|podrías decirme|recuerdame decirte)(?:\\s+(?:con|a|sobre))?\\s+", RegexOption.IGNORE_CASE)
    private val thanks = Regex("[,.!¡¿?\\s]*(?:por favor|porfa|gracias|muchas gracias)[.!]*$", RegexOption.IGNORE_CASE)
    /** Short subjects that mean a question («la hora» → «qué hora es»). */
    private val subjects = mapOf("hora" to "qué hora es", "la hora" to "qué hora es", "fecha" to "qué día es", "la fecha" to "qué día es",
        "el dia" to "qué día es", "el clima" to "qué tiempo hace", "clima" to "qué tiempo hace", "el tiempo" to "qué tiempo hace",
        "mi agenda" to "qué tengo hoy", "la agenda" to "qué tengo hoy")

    /** «Sí» to a question Lucía asked. */
    fun yes(text: String) = Confirmation.yes.matches(Words.normalize(text))


    /** What was said without «Hey Luna», «por favor», «gracias» and «ayúdame con»; a bare subject becomes its question. */
    fun clean(text: String): String {
        var t = text.trim().replace(wake, "").replace(thanks, "").trim().trimEnd('.', '!', ',')
        repeat(2) { t = t.replace(asks, "").replace(thanks, "").trim() }
        t = t.replace(asksForAction) { imperatives.getValue(it.groupValues[1].lowercase()) }
        return subjects[Words.normalize(t)] ?: t
    }

    /** Only split text arguments when their boundaries are known. */
    fun split(text: String, parse: (String) -> Command): List<String> {
        val whole = clean(text)
        if (whole.length > 400 || QuickOrders.webQuery(whole) != null) return listOf(whole)
        val first = parse(whole)
        if (first.action == Action.NOTE || (first.action == Action.MESSAGE && first["text"] != null)) return listOf(whole)
        val parts = whole.split(joints).map(::clean).filter(String::isNotBlank).flatMap { closedSequence(it, parse) }
        return if (parts.size > 1 && parts.all { complete(parse(it)) }) parts else listOf(whole)
    }

    private fun complete(command: Command): Boolean = when (command.action) {
        Action.UNKNOWN -> false
        Action.CALL -> !PhoneSpeech.missingName(command["who"].orEmpty())
        else -> true
    }

    /** A calculation or volume change can finish before a new verb even without a spoken «y». */
    private fun closedSequence(text: String, parse: (String) -> Command): List<String> {
        for (boundary in nextAction.findAll(text).take(12)) {
            val first = clean(text.substring(0, boundary.range.first))
            if (parse(first).action !in closedActions) continue
            val remaining = closedSequence(clean(text.substring(boundary.range.last + 1)), parse)
            return if (remaining.all { complete(parse(it)) }) listOf(first) + remaining else listOf(text)
        }
        return listOf(text)
    }
}
