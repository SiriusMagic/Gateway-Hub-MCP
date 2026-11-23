package com.zirakusa.gatewayhub.assistant

/**
 * Picks who «Ana» or «Ana Pérez» is among the contacts found: the same name first, then one starting the same, and any
 * other only when it is the only one found. Never a guess between several people. Pure.
 */
object People {
    fun best(found: List<Person>, said: String): Person? {
        val s = Words.normalize(said)
        val withNumber = found.filter { it.number.any(Char::isDigit) }
        return withNumber.firstOrNull { Words.normalize(it.name) == s } ?: withNumber.firstOrNull { Words.normalize(it.name).startsWith(s) } ?: withNumber.singleOrNull()
    }
}
