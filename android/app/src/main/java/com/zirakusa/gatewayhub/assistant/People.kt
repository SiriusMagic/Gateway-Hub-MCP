package com.zirakusa.gatewayhub.assistant

import kotlin.math.max

/**
 * Picks who «Ana» or «Ana Pérez» is among the contacts found: the same name first, then one starting the same, then one
 * with all the words said in any order («jefe Disled» is «Disled Jefe»), then one that sounds alike («Dísled», «Vox» for
 * «Box») when it is clearly the closest. Never a guess between several
 * people. Pure.
 */
object People {
    fun best(found: List<Person>, said: String): Person? = candidates(found, said).singleOrNull()

    internal fun candidates(found: List<Person>, said: String): List<Person> {
        val s = Words.normalize(said).removePrefix("mi ")
        if (s.isBlank()) return emptyList()
        // One entry per person: a contact with two numbers is still one person (its first number).
        val withNumber = found.filter { it.number.any(Char::isDigit) }.distinctBy { it.contactKey ?: (it.number.filter(Char::isDigit) + ":" + Words.normalize(it.name)) }
        fun name(p: Person) = Words.normalize(p.name)
        withNumber.filter { name(it) == s }.takeIf { it.isNotEmpty() }?.let { return it }
        withNumber.filter { name(it).startsWith("$s ") }.let { if (it.isNotEmpty()) return it }
        val words = s.split(' ')
        withNumber.filter { p -> words.all { w -> name(p).split(' ').any { it.startsWith(w) } } }.let { if (it.isNotEmpty()) return it }
        return alike(withNumber, s)
    }

    /**
     * Short searches to find names that sound like [said] when the name itself finds nobody: the start of each word, also
     * with the letters that sound the same in Spanish («vo» → «bo», «yo» → «llo»…).
     */
    fun guesses(said: String): List<String> {
        val swaps = listOf("v" to "b", "b" to "v", "s" to "z", "z" to "s", "s" to "c", "c" to "s", "y" to "ll", "ll" to "y", "j" to "g", "g" to "j",
            "k" to "c", "c" to "k", "i" to "y", "y" to "i", "h" to "", "" to "h")
        return Words.normalize(said).split(' ').filter { it.length >= 2 }.take(3).flatMap { w ->
            val start = w.take(2)
            listOf(start) + swaps.mapNotNull { (a, b) -> if (a.isEmpty()) b + start.take(1) else if (start.startsWith(a)) (b + start.drop(a.length)).take(2).takeIf { it.isNotEmpty() } else null }
        }.distinct().take(12)
    }

    /** The one contact that sounds closest to [s], if it is close enough and no other is as close. */
    private fun alike(people: List<Person>, s: String): List<Person> {
        val said = sound(s)
        val scored = people.map { p ->
            val name = Words.normalize(p.name)
            // The whole name, or just as many of its first words as were said («Disled» for «Disled Jefe Pérez»).
            val parts = name.split(' ')
            val short = parts.take(s.split(' ').size).joinToString(" ")
            p to minOf(close(said, sound(name)), close(said, sound(short)))
        }.filter { it.second <= 0.34 }.sortedBy { it.second }
        val best = scored.firstOrNull()?.second ?: return emptyList()
        return scored.takeWhile { it.second - best < 0.1 }.map { it.first }
    }

    /** How a Spanish name sounds, so «Vox», «Box» and «Bos», or «Yesica» and «Jessica», come out the same. */
    fun sound(text: String): String {
        var t = Words.normalize(text).replace(" ", "")
        listOf("ch" to "C", "ll" to "y", "qu" to "k", "gue" to "ge", "gui" to "gi", "ce" to "se", "ci" to "si", "ge" to "je",
            "gi" to "ji", "sh" to "C", "ph" to "f", "th" to "t").forEach { (a, b) -> t = t.replace(a, b) }
        t = t.map { c -> when (c) { 'v' -> 'b'; 'z' -> 's'; 'c' -> 'k'; 'q' -> 'k'; 'x' -> 's'; 'w' -> 'u'; 'i' -> 'i'; 'h' -> ' '; else -> c } }
            .joinToString("").replace(" ", "").replace("y", "i").replace("j", "J")
        return t.replace(Regex("(.)\\1+"), "$1")
    }

    /** 0 = the same, 1 = nothing alike: the edit distance over the longer one. */
    private fun close(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 1.0
        val d = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = d[0]; d[0] = i
            for (j in 1..b.length) {
                val keep = d[j]
                d[j] = minOf(d[j] + 1, d[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = keep
            }
        }
        return d[b.length].toDouble() / max(a.length, b.length)
    }
}
