package com.zirakusa.gatewayhub.assistant

import java.math.BigDecimal
import java.math.MathContext

/** Phrases Lucía can answer without waiting for a model or calling another app. */
internal object QuickOrders {
    private val greeting = Regex("^(?:hola(?: (?:luc[ií]a|c[oó]mo (?:est[aá]s|listas)|qu[eé] tal))?|c[oó]mo est[aá]s|qu[eé] tal)$", RegexOption.IGNORE_CASE)
    private val web = Regex("^(?:busca(?:me|r)?|consulta(?:r)?|investiga) en (?:internet|(?:la )?web|google)(?: (?:sobre )?(.+))?$", RegexOption.IGNORE_CASE)
    private val image = Regex("^(?:genera|crea|dibuja|haz(?:me)?) (?:una |un )?(?:imagen|dibujo|ilustracion)(?: .*|$)")
    private val math = Regex("^(?:entonces +)?calcul(?:a|ar|es) +(.+)$", RegexOption.IGNORE_CASE)
    /** «suma dos más dos», «resta 3 a 10», «multiplica 3 por 4», «divide 10 entre 2»: the verb says the operation. */
    private val verb = Regex("^(?:entonces +)?(sum|rest|multiplic|divid)(?:a|e|ar|ir|ame|eme)? +(.+)$", RegexOption.IGNORE_CASE)
    /** Only numbers and operations («dos más dos», «8 por 3»): a calculation without «calcula». */
    private val bare = Regex("^[0-9.]+(?: ?(?:mas|menos|por|entre|multiplicado por|dividido (?:por|entre)|[+*/x-]) ?[0-9.]+)+$")
    private val number = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)")
    private val spelling = mapOf("multiplicado por" to "*", "dividido entre" to "/", "dividido por" to "/",
        "por" to "*", "entre" to "/", "mas" to "+", "menos" to "-", "x" to "*")

    fun parse(raw: String): Command? {
        val phrase = raw.trim().trim('¿', '?', '¡', '!', '.').replace(Regex("\\s+"), " ").trim()
        webQuery(phrase)?.let { return Command(Action.WEB_SEARCH, mapOf("query" to it)) }
        math.matchEntire(phrase)?.let { match ->
            return calculate(match.groupValues[1])?.let { Command(Action.CALCULATE, it) }
        }
        verb.matchEntire(phrase)?.let { match ->
            return byVerb(match.groupValues[1].lowercase(), match.groupValues[2])?.let { Command(Action.CALCULATE, it) }
        }
        if (greeting.matches(phrase.replace(",", ""))) return quick("greeting")
        val normalized = Words.normalize(phrase)
        if (bare.matches(normalized)) calculate(phrase)?.let { return Command(Action.CALCULATE, it) }
        return when {
            image.matches(normalized) -> quick("image_unavailable")
            normalized in setOf("estoy triste", "me siento triste", "me siento solo", "me siento sola") -> quick("sad")
            normalized in setOf("estoy feliz", "me siento feliz", "estoy contento", "estoy contenta") -> quick("happy")
            else -> null
        }
    }

    fun webQuery(phrase: String): String? = web.matchEntire(phrase)?.groupValues?.get(1)

    private fun quick(kind: String) = Command(Action.CHAT, mapOf("quick" to kind))

    /**
     * The verb's operation between two numbers: «suma 5 y 3» / «suma 5 con 3» is 5 + 3, «resta 3 a 10» / «resta 3 de 10»
     * is 10 − 3, «multiplica 3 y 4» is 3 × 4, «divide 10 en 2» is 10 ÷ 2. Already with its signs («suma dos más dos»), as
     * said. Null when only one number is left (nothing to operate).
     */
    private fun byVerb(verb: String, raw: String): Map<String, String>? {
        val t = Words.normalize(raw.replace(Regex("(?<=\\d),(?=\\d)"), "§")).replace('§', '.')
            .removePrefix("los numeros ").removePrefix("el numero ")
        val two = Regex("^([0-9]+(?:\\.[0-9]+)?) (y|con|a|de|en) ([0-9]+(?:\\.[0-9]+)?)$").matchEntire(t)
        val expr = two?.destructured?.let { (a, joint, b) ->
            when (verb) {
                "sum" -> "$a mas $b"
                "rest" -> if (joint == "a" || joint == "de") "$b menos $a" else "$a menos $b"
                "multiplic" -> "$a por $b"
                else -> "$a entre $b"
            }
        } ?: raw.takeIf { Regex("(^| )(?:mas|menos|por|entre|x)( |$)|[+*/-]").containsMatchIn(t) } ?: return null
        return calculate(expr)
    }

    private fun calculate(raw: String): Map<String, String>? {
        if (raw.length > 120 || raw.any { it == '(' || it == ')' }) return null
        // Keep decimal separators while using the same spoken-number normalization as other orders.
        val decimal = Regex("(?<=\\d)[.,](?=\\d)")
        var expr = Words.normalize(raw.replace(decimal, "§"))
            .replace('§', '.').replace('×', '*').replace('÷', '/')
            .replace(Regex("\\by (?=mas\\b|menos\\b)"), "")
        expr = expr.replace(Regex("""\b([2-9]0) y (1|un|uno|[2-9])\b""")) { match ->
            val unit = match.groupValues[2].toIntOrNull() ?: 1
            (match.groupValues[1].toInt() + unit).toString()
        }.replace(Regex("""\b(?:un|uno)\b"""), "1")
        for ((word, symbol) in spelling) expr = expr.replace(Regex("\\b$word\\b"), symbol)
        expr = expr.replace(Regex("(?<=\\d)x(?=[+\\-]?[0-9])"), "*")
        if (Regex("[0-9.] +[0-9.]").containsMatchIn(expr)) return null

        // Evaluate multiplication and division before adding each term to the total.
        return try {
            val parts = expr.replace(" ", "")
            val first = number.matchAt(parts, 0) ?: return null
            var term = first.value.toBigDecimal()
            var sum = BigDecimal.ZERO
            var index = first.range.last + 1
            while (index < parts.length) {
                val op = parts[index]
                val match = number.matchAt(parts, index + 1) ?: return null
                val value = match.value.toBigDecimal()
                when (op) {
                    '*' -> term *= value
                    '/' -> term = term.divide(value, MathContext.DECIMAL128)
                    '+', '-' -> {
                        sum += term
                        term = if (op == '+') value else value.negate()
                    }
                    else -> return null
                }
                index = match.range.last + 1
            }
            mapOf("calculation" to (sum + term).stripTrailingZeros().toPlainString())
        } catch (_: ArithmeticException) {
            mapOf("math_error" to "division_by_zero")
        }
    }
}
