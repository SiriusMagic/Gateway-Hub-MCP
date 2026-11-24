package com.zirakusa.gatewayhub.assistant

import java.math.BigDecimal
import java.math.MathContext

/** Phrases Lucía can answer without waiting for a model or calling another app. */
internal object QuickOrders {
    private val greeting = Regex("^(?:hola(?: (?:luc[ií]a|c[oó]mo (?:est[aá]s|listas)|qu[eé] tal))?|c[oó]mo est[aá]s|qu[eé] tal)$", RegexOption.IGNORE_CASE)
    private val web = Regex("^(?:busca(?:me|r)?|consulta(?:r)?|investiga) en (?:internet|(?:la )?web|google)(?: (?:sobre )?(.+))?$", RegexOption.IGNORE_CASE)
    private val image = Regex("^(?:genera|crea|dibuja|haz(?:me)?) (?:una |un )?(?:imagen|dibujo|ilustracion)(?: .*|$)")
    private val math = Regex("^(?:entonces +)?calcula +(.+)$", RegexOption.IGNORE_CASE)
    private val number = Regex("[+-]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)")
    private val spelling = mapOf("multiplicado por" to "*", "dividido entre" to "/", "dividido por" to "/",
        "por" to "*", "entre" to "/", "mas" to "+", "menos" to "-", "x" to "*")

    fun parse(raw: String): Command? {
        val phrase = raw.trim().trim('¿', '?', '¡', '!', '.').replace(Regex("\\s+"), " ").trim()
        webQuery(phrase)?.let { return Command(Action.WEB_SEARCH, mapOf("query" to it)) }
        math.matchEntire(phrase)?.let { match ->
            return calculate(match.groupValues[1])?.let { Command(Action.CALCULATE, it) }
        }
        if (greeting.matches(phrase.replace(",", ""))) return quick("greeting")
        val normalized = Words.normalize(phrase)
        return when {
            image.matches(normalized) -> quick("image_unavailable")
            normalized in setOf("estoy triste", "me siento triste", "me siento solo", "me siento sola") -> quick("sad")
            normalized in setOf("estoy feliz", "me siento feliz", "estoy contento", "estoy contenta") -> quick("happy")
            else -> null
        }
    }

    fun webQuery(phrase: String): String? = web.matchEntire(phrase)?.groupValues?.get(1)

    private fun quick(kind: String) = Command(Action.CHAT, mapOf("quick" to kind))

    private fun calculate(raw: String): Map<String, String>? {
        if (raw.length > 120 || raw.any { it == '(' || it == ')' }) return null
        // Keep decimal separators while using the same spoken-number normalization as other orders.
        val decimal = Regex("(?<=\\d)[.,](?=\\d)")
        var expr = Words.normalize(raw.replace(decimal, "§"))
            .replace('§', '.').replace('×', '*').replace('÷', '/')
            .replace(Regex("\\by (?=mas\\b|menos\\b)"), "")
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
