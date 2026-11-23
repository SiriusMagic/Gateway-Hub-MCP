package com.zirakusa.gatewayhub.assistant

import java.text.Normalizer

/** One line of a count: what (singular, as first said) and how many. */
data class Tallied(val key: String, val label: String, val n: Int)

/** What was said while counting. In [Add], an empty phrase means «the last thing counted» («otro», «tres más»). */
sealed interface TallyOrder {
    data class Add(val items: List<Pair<String, Int>>) : TallyOrder
    data class Remove(val item: String?, val n: Int) : TallyOrder
    data object Undo : TallyOrder
    data class Read(val item: String?) : TallyOrder
    data object Finish : TallyOrder
    data object Cancel : TallyOrder
    data object Nothing : TallyOrder
}

/**
 * «Modo conteo»: things said aloud are counted («tomacorriente, tomacorriente, interruptor simple» → 2 and 1), with
 * quantities («cinco focos»), «otro», corrections («quita uno», «borra el último»), «cuántos van» and «termina». Plural
 * and singular count together, and the words the small voice model splits («toma corriente») are joined. Pure.
 */
object TallyWords {
    private val numbers = mapOf(
        "un" to 1, "una" to 1, "uno" to 1, "otro" to 1, "otra" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "seis" to 6,
        "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15,
        "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21, "veintidos" to 22,
        "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25, "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28,
        "veintinueve" to 29, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50,
    )
    /** Words that end in «s» but are not plurals. */
    private val keepS = numbers.keys + setOf("mas", "gas", "bus", "pies", "lunes", "martes", "miercoles", "jueves", "viernes", "tesis", "crisis", "virus", "atlas")
    private val articles = setOf("el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "mas", "y", "otro", "otra", "otros", "otras")
    private val fillers = setOf("oye", "lucia", "luna", "nova", "kira", "eh", "este", "bueno", "ahora", "ya", "y", "pues", "a", "ver")

    /** Common things an electrician counts: their nice name (the key is what both singular and plural become). */
    val known: Map<String, String> = listOf(
        "Tomacorriente", "Tomacorriente doble", "Tomacorriente especial", "Interruptor simple", "Interruptor doble", "Interruptor triple",
        "Interruptor de tres vías", "Interruptor", "Foco", "Luminaria", "Lámpara", "Aplique", "Reflector", "Plafón", "Spot", "Panel led",
        "Sensor de movimiento", "Timbre", "Breaker", "Tablero", "Caja", "Cajetín", "Salida especial", "Punto de luz", "Punto de datos",
        "Punto de televisión", "Ducha eléctrica", "Ventilador", "Extractor", "Cocina eléctrica", "Calefón", "Enchufe", "Apagador",
    ).associateBy { key(it) }

    /** Lower case, no punctuation or «[unk]», single spaces (accents kept: they go to the note). */
    fun clean(raw: String) = raw.lowercase().replace("[unk]", " ").replace(Regex("[¿?¡!.,;:\"()]"), " ").replace(Regex("\\s+"), " ").trim()

    fun plain(s: String): String = Normalizer.normalize(s.lowercase().replace('ñ', '\u0001'), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace('\u0001', 'ñ')

    /** «interruptores» → «interruptor», «luces» → «luz», «simples» → «simple», «vías» → «vía»; «tres» stays. */
    fun singular(word: String): String {
        val p = plain(word)
        if (p.length < 4 || p in keepS || !p.endsWith("s") || p.endsWith("is") || p.endsWith("us")) return word
        if (p.endsWith("ces")) return word.dropLast(3) + "z"
        if (p.endsWith("es")) {
            val stem = p.dropLast(2)
            if (stem.length >= 2 && stem.last() in "rlndjy" && stem[stem.length - 2] in "aeiou") return word.dropLast(2)
        }
        return word.dropLast(1)
    }

    private fun joinSplit(s: String) = s.replace(Regex("(?<=^| )tomas? corrientes?(?= |$)"), "tomacorriente")

    /** What singular and plural, with or without accents, have in common. */
    fun key(phrase: String) = joinSplit(plain(clean(phrase)).split(' ').filter { it.isNotEmpty() }.joinToString(" ") { plain(singular(it)) })

    /** How a new thing is written in the list: its known name, or as said, singular, with a capital. */
    fun label(phrase: String): String {
        known[key(phrase)]?.let { return it }
        val words = clean(phrase).split(' ').filter { it.isNotEmpty() }.map(::singular)
        return joinSplit(words.joinToString(" ")).replaceFirstChar { it.uppercase() }
    }

    private fun number(word: String) = word.toIntOrNull()?.takeIf { it in 1..999 } ?: numbers[word]

    /** Understands one phrase while counting. [counted] are the keys already in the list (to split «foco foco foco»). */
    fun order(raw: String, counted: Collection<String> = emptyList()): TallyOrder {
        val said = clean(raw).split(' ').filter { it.isNotEmpty() }.toMutableList()
        while (said.isNotEmpty() && plain(said.first()) in fillers) said.removeAt(0)
        val t = plain(said.joinToString(" "))
        if (t.isEmpty()) return TallyOrder.Nothing
        if (Regex("^(termina|terminar|termine|terminamos|terminado|listo|lista|fin|finaliza|finalizar|guarda|guardar|guardalo|ya esta|eso es todo|ya termine)( (el )?conteo)?$").matches(t)) return TallyOrder.Finish
        if (Regex("^(cancela|cancelar|cancelalo|descarta|descartar|salir sin guardar|borra todo)( (el )?conteo)?$").matches(t)) return TallyOrder.Cancel
        if (Regex("^(deshacer|deshaz|borra el ultimo|borra eso|borralo|quita el ultimo|quita eso|me equivoque|anula|anula eso|anula el ultimo|eso no)$").matches(t)) return TallyOrder.Undo
        if (Regex("^(total|lee|leeme|repite|resumen|cuanto llevo|cuantos llevo|cuantos van|cuantas van|cuanto va|cuantos hay|cuantas hay|cuantos son|cuantos tengo)$").matches(t)) return TallyOrder.Read(null)
        Regex("^cuant[oa]s? (?:(?:van|llevo|hay|tengo|son) )?(?:de )?(.+)$").find(t)?.let { m -> return TallyOrder.Read(m.groupValues[1]) }
        Regex("^(quita|quitale|resta|restale|menos|saca|sacale|borra) ?(.*)$").find(t)?.let { m ->
            val rest = m.groupValues[2].split(' ').filter { it.isNotEmpty() }
            val n = rest.firstOrNull()?.let(::number)
            val item = (if (n != null) rest.drop(1) else rest).dropWhile { it in articles }.joinToString(" ").ifEmpty { null }
            return TallyOrder.Remove(item, n ?: 1)
        }
        val items = ArrayList<Pair<String, Int>>()
        // «dos focos y un interruptor»: each part by itself.
        val parts = ArrayList<MutableList<String>>().apply { add(ArrayList()) }
        said.forEach { w -> if (plain(w) == "y") parts.add(ArrayList()) else parts.last().add(w) }
        for (part in parts) {
            if (part.isEmpty()) continue
            var n = number(plain(part.first()))
            var words = if (n != null) part.drop(1) else part.toList()
            if (words.isNotEmpty() && plain(words.last()) == "mas") words = words.dropLast(1)
            words = words.dropWhile { plain(it) in articles }
            if (words.isEmpty()) { if (n != null) items += "" to n; continue }
            if (words.none { plain(it).length >= 3 && plain(it) !in articles }) continue
            if (n == null) n = 1
            items += split(words, counted).mapIndexed { i, (phrase, times) -> phrase to (if (i == 0) n else 1) * times }
        }
        return if (items.isEmpty()) TallyOrder.Nothing else TallyOrder.Add(items)
    }

    /**
     * «toma corriente toma corriente toma corriente» said without pauses → 3 of one thing; «foco interruptor simple» →
     * one of each when every piece is something known. Otherwise the whole phrase is one thing.
     */
    private fun split(words: List<String>, counted: Collection<String>): List<Pair<String, Int>> {
        val keys = key(words.joinToString(" ")).split(' ')   // «toma corriente» joins into one token here
        val shown = joinSplit(words.joinToString(" ")).split(' ')
        if (keys.size != shown.size || keys.size < 2) return listOf(words.joinToString(" ") to 1)
        val vocabulary = (known.keys + counted).map { it.split(' ') }.sortedByDescending { it.size }
        val found = ArrayList<Pair<String, Int>>()
        var i = 0
        while (i < keys.size) {
            val k = vocabulary.firstOrNull { v -> v.size <= keys.size - i && keys.subList(i, i + v.size) == v } ?: break
            found += shown.subList(i, i + k.size).joinToString(" ") to 1
            i += k.size
        }
        if (i == keys.size && found.size > 1) return found.groupBy { key(it.first) }.values.map { g -> g.first().first to g.size }
        for (p in 1..keys.size / 2) {
            if (keys.size % p == 0 && keys.indices.all { keys[it] == keys[it % p] }) return listOf(shown.subList(0, p).joinToString(" ") to keys.size / p)
        }
        return listOf(words.joinToString(" ") to 1)
    }

    /** Words the limited recognizer listens for while counting: orders, numbers, the known things and what is in the list. */
    fun grammar(lines: List<Tallied> = emptyList()): String {
        val base = """
            termina terminar listo guarda guardar fin cancela cancelar descarta deshacer borra quita resta menos saca último eso me
            equivoqué anula cuántos cuántas van llevo hay total lee repite otro otra más el la los las de del y un una uno dos tres
            cuatro cinco seis siete ocho nueve diez once doce trece catorce quince veinte treinta toma tomas corriente corrientes
        """.trim().split(Regex("\\s+"))
        val things = (known.values + lines.map { it.label }).flatMap { clean(it).split(' ') }.filter { it.length >= 2 }
        val plurals = things.map { w -> if (plain(w).last() in "aeiou") w + "s" else if (w.endsWith("z")) w.dropLast(1) + "ces" else w + "es" }
        return ((base + things + plurals).distinct().sorted() + "[unk]").joinToString(",", "[", "]") { "\"" + it.replace("\\", "").replace("\"", "") + "\"" }
    }

    /** While counting, the limited recognizer wins unless it heard something outside its words. */
    fun pick(limited: String, free: String): String {
        val l = limited.replace("[unk]", " ").replace(Regex("\\s+"), " ").trim()
        return if ((l.isEmpty() || "[unk]" in limited) && free.isNotBlank()) free.trim() else l
    }
}

/** A count in progress: its lines in the order they were first said, and each change so «borra el último» can undo it. */
class Tally(val title: String = "") {
    enum class Kind { ADDED, REMOVED, UNDONE, READ, FINISH, CANCEL, NOTHING, NOT_FOUND }
    /** What a phrase did, and the lines it touched (or all of them, for «cuántos van»). */
    data class Outcome(val kind: Kind, val lines: List<Tallied> = emptyList())

    var lines: List<Tallied> = emptyList(); private set
    val total get() = lines.sumOf { it.n }
    private val steps = ArrayList<List<Pair<String, Int>>>()
    private val labels = HashMap<String, String>()
    private var lastKey: String? = null

    fun hear(text: String): Outcome = apply(TallyWords.order(text, lines.map { it.key }))

    fun apply(order: TallyOrder): Outcome = when (order) {
        is TallyOrder.Add -> {
            val changes = order.items.mapNotNull { (phrase, n) ->
                val key = if (phrase.isEmpty()) lastKey else TallyWords.key(phrase)
                if (key == null) null else { if (phrase.isNotEmpty()) labels.getOrPut(key) { TallyWords.label(phrase) }; key to n }
            }
            if (changes.isEmpty()) Outcome(Kind.NOTHING) else Outcome(Kind.ADDED, change(changes))
        }
        is TallyOrder.Remove -> {
            val key = order.item?.let(TallyWords::key) ?: lastKey
            val line = lines.firstOrNull { it.key == key }
            if (line == null) Outcome(Kind.NOT_FOUND) else Outcome(Kind.REMOVED, change(listOf(line.key to -minOf(order.n, line.n))))
        }
        TallyOrder.Undo -> steps.removeLastOrNull()?.let { last -> Outcome(Kind.UNDONE, edit(last.map { (k, n) -> k to -n })) } ?: Outcome(Kind.NOT_FOUND)
        is TallyOrder.Read -> if (order.item == null) Outcome(Kind.READ, lines)
            else lines.firstOrNull { it.key == TallyWords.key(order.item) }?.let { Outcome(Kind.READ, listOf(it)) } ?: Outcome(Kind.NOT_FOUND)
        TallyOrder.Finish -> Outcome(Kind.FINISH, lines)
        TallyOrder.Cancel -> Outcome(Kind.CANCEL)
        TallyOrder.Nothing -> Outcome(Kind.NOTHING)
    }

    /** The + and − buttons of a line. */
    fun bump(key: String, delta: Int): Outcome {
        val line = lines.firstOrNull { it.key == key } ?: return Outcome(Kind.NOT_FOUND)
        val d = if (delta < 0) -minOf(-delta, line.n) else delta
        return Outcome(if (d >= 0) Kind.ADDED else Kind.REMOVED, change(listOf(key to d)))
    }

    private fun change(changes: List<Pair<String, Int>>): List<Tallied> {
        steps += changes
        changes.lastOrNull { it.second > 0 }?.let { lastKey = it.first }
        return edit(changes)
    }

    private fun edit(changes: List<Pair<String, Int>>): List<Tallied> {
        val now = lines.toMutableList()
        for ((key, n) in changes) {
            val i = now.indexOfFirst { it.key == key }
            if (i < 0) { if (n > 0) now += Tallied(key, labels[key] ?: TallyWords.label(key), n) }
            else if (now[i].n + n <= 0) now.removeAt(i) else now[i] = now[i].copy(n = now[i].n + n)
        }
        lines = now
        return changes.map { it.first }.distinct().map { k -> lines.firstOrNull { it.key == k } ?: Tallied(k, labels[k] ?: k, 0) }
    }

    /** The count as a Notas table (Notas draws «| a | b |» lines as a table). */
    fun table(thing: String, count: String, totalWord: String) = buildString {
        append("| ").append(thing).append(" | ").append(count).append(" |\n")
        lines.forEach { append("| ").append(it.label.replace("|", "/")).append(" | ").append(it.n).append(" |\n") }
        append("| ").append(totalWord).append(" | ").append(total).append(" |")
    }
}
