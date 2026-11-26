package com.zirakusa.gatewayhub.assistant

/** The reviewed vocabulary is packaged with Arka. Each entry uses the existing music grammar. */
internal object IntentRules {
    private val allowed = Regex("(?:arranca|empieza|inicia)(?: la)?")
    val musicPrefix: Regex? by lazy {
        val entries = IntentRules::class.java.getResourceAsStream("/intent-music-prefixes.txt")?.bufferedReader()?.useLines { lines ->
            lines.map(String::trim).filter { allowed.matches(it) }.distinct().toList()
        }.orEmpty()
        if (entries.isEmpty()) null else Regex(
            "^(?:${entries.joinToString("|") { Regex.escape(it) }}) (?=(?:música|musica)(?:\\s|$))",
            RegexOption.IGNORE_CASE
        )
    }
}
