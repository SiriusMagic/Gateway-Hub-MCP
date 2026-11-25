package com.zirakusa.gatewayhub.assistant

import java.text.Normalizer

/** Words the general recognizer can mistake for a contact in a call command. */
internal object PhoneSpeech {
    private val fragments = Regex("^(?:(?:a |al |y )?(?:qr|q r|hacia|cuelga|colgar|cuelgue|cuelgale|y|a|al))$")

    fun missingName(name: String): Boolean = name.isBlank() || fragments.matches(Words.normalize(name))

    fun answerName(text: String): String? {
        val candidate = text.trim().trimEnd('.', '!', '?', ',')
        if (missingName(candidate) || Confirmation.callAnswer(candidate) != null) return null
        return candidate.takeIf { it.length <= 60 && it.matches(Regex("""[\p{L}\d+ .'-]+""")) }
    }

    private val nameMarks = Regex("""[\p{M}]+""")
    private val nameSpaces = Regex("""\s+""")

    private fun nameKey(name: String) = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
        .replace(nameMarks, "").replace(nameSpaces, " ").trim()

    private fun knownName(names: List<String>, said: String): String? {
        val key = nameKey(said)
        if (key.isBlank()) return null
        return names.firstOrNull { it == key } ?: names.filter { it.startsWith("$key ") }.singleOrNull()
    }

    fun contactName(limited: String, free: String, contacts: List<String>): String {
        val a = answerName(limited)
        val b = answerName(free)
        val names = contacts.map(::nameKey).distinct()
        val knownA = a?.let { knownName(names, it) }
        val knownB = b?.let { knownName(names, it) }
        return when {
            knownA != null && knownB != null && knownA != knownB -> ""
            knownA != null -> a.orEmpty()
            knownB != null -> b.orEmpty()
            b != null && Parser.parse(b).action == Action.UNKNOWN -> b
            else -> ""
        }
    }

    fun preferredCall(
        limited: Command, limitedText: String, free: Command, freeText: String, contacts: List<String>,
    ): String? {
        if (limited.action != Action.CALL && free.action != Action.CALL) return null
        val names = contacts.map(::nameKey).distinct()
        fun match(command: Command): String? {
            if (command.action != Action.CALL) return null
            val said = command["who"] ?: return null
            return knownName(names, said).takeUnless { missingName(said) }
        }
        val a = match(limited)
        val b = match(free)
        return when {
            a != null && b != null && a != b -> "llama"
            a != null -> limitedText
            b != null -> freeText
            limited.action == Action.CALL && missingName(limited["who"].orEmpty()) &&
                free.action == Action.CALL && !missingName(free["who"].orEmpty()) -> freeText
            else -> null
        }
    }
}
