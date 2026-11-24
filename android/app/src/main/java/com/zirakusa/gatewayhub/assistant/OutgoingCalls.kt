package com.zirakusa.gatewayhub.assistant

internal object Confirmation {
    val yes = Regex("^(?:si|claro|confirmo|confirma|hazlo|dale|adelante|ok|okay|okey|de acuerdo|vale|por supuesto|si por favor|si hazlo|correcto|eso)$")
    val no = Regex("^(?:no|cancela|cancelar|mejor no|no gracias|deja|dejalo|olvidalo|para)$")
}

/** Keeps the specific number in memory only while awaiting consent or Android's permission result. */
internal class OutgoingCalls(private val hands: Hands, private val clock: () -> Long) {
    private data class Request(val person: Person, val until: Long)

    private var confirmation: Request? = null
    private var permission: Request? = null

    val asking get() = confirmation?.let { clock() <= it.until } == true

    fun cancel() {
        confirmation = null
        permission = null
    }

    fun request(person: Person): Pair<Reply, Boolean> {
        confirmation = Request(person, clock() + Brain.CONFIRM_MS)
        return Reply(Say.CONFIRM_CALL, listOf(person.name)) to true
    }

    fun answer(heard: String): Pair<Reply, Boolean>? {
        permission = null
        val request = confirmation ?: return null
        confirmation = null
        if (clock() > request.until) return null
        val text = Words.normalize(heard)
        return when {
            Confirmation.yes.matches(text) -> place(request.person)
            Confirmation.no.matches(text) -> Reply(Say.CANCELLED) to true
            else -> null
        }
    }

    fun finishPermission(granted: Boolean): Pair<Reply, Boolean>? {
        val request = permission ?: return null
        permission = null
        if (clock() > request.until) return null
        return if (granted) place(request.person, requestPermission = false) else dial(request.person)
    }

    fun dial(person: Person): Pair<Reply, Boolean> =
        if (hands.dial(person.number)) Reply(Say.CALLING, listOf(person.name)) to true
        else Reply(Say.FAILED, listOf(Catalog.byId("telefono").name)) to false

    private fun place(person: Person, requestPermission: Boolean = true): Pair<Reply, Boolean> =
        try {
            if (hands.placeCall(person.number)) Reply(Say.CALL_STARTED, listOf(person.name)) to true else dial(person)
        } catch (_: Hands.NoCallPermission) {
            if (requestPermission) {
                permission = Request(person, clock() + Brain.CONFIRM_MS)
                Reply(Say.NEED_CALL_PERMISSION) to false
            } else dial(person)
        }
}
