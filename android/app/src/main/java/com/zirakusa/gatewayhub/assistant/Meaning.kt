package com.zirakusa.gatewayhub.assistant

import kotlin.math.sqrt

/**
 * Layer 2 of Lucía: what was said by its meaning, without the model. When the rules don't know a phrase, it is said
 * again in Lucía's own words («quiero escuchar alone» → «pon alone», «despiértame a las 6» → «pon una alarma a las 6»)
 * and the rules try once more; [rank] orders her examples by how alike they are (letters and words), so the model, when
 * it is needed, reads only the closest ones and answers sooner. Pure and instant (no model, no internet).
 */
object Meaning {
    private fun r(pattern: String) = Regex(pattern)

    /** Wrappers that add nothing to the order («¿me puedes…?», «quisiera saber…», «…por favor»). */
    private val wrappers = listOf(
        r("^(?:me )?(?:puedes|podrias|podras|pudieras|quieres|quisieras)(?: por favor)? "),
        r("^(?:quisiera que|quiero que|necesito que|me gustaria que|me haces el favor de|haz el favor de|ayudame a) "),
        r("^(?:me dices|me puedes decir|podrias decirme|quisiera saber|quiero saber|me gustaria saber|sabes|tu sabes) "),
        r("(?: por favor| gracias| porfavor)$"),
        // «¿Me (puedes) poner…?»: the «puedes» is already gone, the «me» is left alone in front of the verb.
        r("^me (?=(?:poner|pones|abrir|abres|decir|dices|dar|das|mostrar|muestras|llamar|buscar|buscas|anotar|apuntar|despertar|avisar)\\b)"),
    )

    /** Ways of saying an order → Lucía's own words (applied on normalized text: no accents, digits). */
    private val rewrites: List<Pair<Regex, String>> = listOf(
        // Music: everything ends as «pon …», which Stone's tools look up by title, artist or album.
        r("^(?:quiero|quisiera|me gustaria|me apetece|tengo ganas de|vamos a) (?:oir|escuchar|poner|reproducir)(?: un poco de| algo de)? ") to "pon ",
        r("^(?:hazme sonar|haz sonar|dale a|dale play a|suena|poner|ponte|ponnos|tocanos|reproduceme|reproducir|tocame|colocame|coloca) ") to "pon ",
        r("^pon (?:la |una )?(?:cancion|tema|rola)(?: llamada| que se llama| de nombre| titulada)? ") to "pon musica ",
        // The verb as an infinitive («abrir la cámara», «llamar a Luis», «buscar mis notas»).
        r("^abrir ") to "abre ", r("^llamar a ") to "llama a ", r("^buscar ") to "busca ", r("^anotar ") to "anota ",
        r("^(?:mostrar|mostrarme) ") to "muestrame ", r("^borrar ") to "borra ",
        // Apps
        r("^(?:abreme|lanza|lanzame|inicia|iniciame|ejecuta|arranca|entra a|entra en|ve a|vamos a|llevame a) (?:la app de |la aplicacion de |la app |la aplicacion )?") to "abre ",
        // Notes and reminders
        r("^(?:apunta|apuntame|toma nota de|toma nota|guarda una nota que diga|crea una nota que diga|haz una nota de|anotame) ") to "anota ",
        r("^(?:despiertame|levantame|ponme una alarma|ponme la alarma|programa una alarma|programa la alarma|alarma) (?:a las |para las |a la )?") to "pon una alarma a las ",
        r("^(?:avisame|recuerdame|ponme un temporizador de|cuenta|cronometra) (?:en |dentro de )?") to "pon un temporizador de ",
        // Calls and messages: still asked first by the rules.
        r("^(?:marca a|marcale a|telefonea a|telefonea|comunicame con|ponme con|quiero hablar con|llamame a|hazle una llamada a|haz una llamada a) ") to "llama a ",
        r("^(?:escribele a|enviale un mensaje a|mandale un mensaje a|envia un mensaje a|escribe un mensaje a|textea a|escribele un sms a) ") to "manda un mensaje a ",
        // Changes and looks
        r("^(?:elimina|suprime|tira) ") to "borra ",
        r("^(?:ensename|enseñame|quiero ver|dejame ver|muestra) ") to "muestrame ",
        r("^(?:cual es|como esta|que tal esta|que tal) (?:el )?(?:clima|tiempo)(?: hoy| ahora)?$") to "que tiempo hace",
    )

    /** Lucía's own words for [text], most likely first; empty when no way of saying it applies. */
    fun candidates(text: String): List<String> {
        val t = Words.normalize(text)
        var bare = t
        repeat(2) { wrappers.forEach { w -> bare = bare.replace(w, "").trim() } }
        val out = LinkedHashSet<String>()
        if (bare != t && bare.isNotBlank()) out += bare
        for (base in listOf(bare, t)) {
            var once = base
            rewrites.forEach { (from, to) -> if (from.containsMatchIn(once)) once = from.replaceFirst(once, to).trim() }
            if (once != base) out += once
        }
        return out.filter { it.isNotBlank() && it != t }
    }

    /** The first rewording the rules understand ([parse] gives UNKNOWN when they don't), or null. */
    fun understand(text: String, parse: (String) -> Command): Command? =
        candidates(text).asSequence().map(parse).firstOrNull { it.action != Action.UNKNOWN }

    /** Lucía's examples closest to [query] (letter trigrams and whole words), most alike first. */
    fun rank(query: String, examples: List<String>, max: Int): List<String> {
        val q = Words.normalize(candidates(query).firstOrNull() ?: query)
        val qv = vector(q); val qw = words(q)
        return examples.map { e -> val n = Words.normalize(e); e to (cosine(qv, vector(n)) + 0.5 * overlap(qw, words(n))) }
            .sortedByDescending { it.second }.take(max).map { it.first }
    }

    private fun words(t: String) = t.split(' ').filter { it.length > 2 }.toSet()
    private fun overlap(a: Set<String>, b: Set<String>) = if (a.isEmpty() || b.isEmpty()) 0.0 else a.count { it in b }.toDouble() / sqrt(a.size * b.size.toDouble())
    private fun vector(t: String): Map<String, Int> = " $t ".windowed(3).groupingBy { it }.eachCount()
    private fun cosine(a: Map<String, Int>, b: Map<String, Int>): Double {
        val dot = a.entries.sumOf { (k, v) -> v * (b[k] ?: 0) }.toDouble()
        val na = sqrt(a.values.sumOf { it * it }.toDouble()); val nb = sqrt(b.values.sumOf { it * it }.toDouble())
        return if (na == 0.0 || nb == 0.0) 0.0 else dot / (na * nb)
    }
}
