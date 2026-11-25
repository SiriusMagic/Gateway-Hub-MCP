package com.zirakusa.gatewayhub.assistant

internal object Confirmation {
    val yes = Regex("^(?:si|claro|confirmo|confirma|hazlo|dale|adelante|ok|okay|okey|de acuerdo|vale|por supuesto|si por favor|si hazlo|correcto|eso)$")
    private val callYes = Regex("^(?:y si|si (?:llama|llamale))$")
    private val callNo = Regex("^(?:y no|no llames)$")

    fun spokenCallAnswer(heard: String): Boolean? {
        val text = Words.normalize(Intents.clean(heard))
        return when {
            text == "si" || callYes.matches(text) -> true
            no.matches(text) || callNo.matches(text) -> false
            else -> null
        }
    }

    fun callAnswer(heard: String): Boolean? =
        spokenCallAnswer(heard) ?: true.takeIf { yes.matches(Words.normalize(heard)) }

    val no = Regex("^(?:no|cancela|cancelar|mejor no|no gracias|deja|dejalo|olvidalo|para)$")
}

/** Keeps the specific number in memory only while awaiting consent or Android's permission result. */
internal class OutgoingCalls(private val hands: Hands, private val clock: () -> Long) {
    private data class Request(val person: Person, val speaker: Boolean, val until: Long)

    private var confirmation: Request? = null
    private var permission: Request? = null

    val asking get() = confirmation?.let { clock() <= it.until } == true

    fun cancel() {
        confirmation = null
        permission = null
    }

    fun request(person: Person, speaker: Boolean): Pair<Reply, Boolean> {
        confirmation = Request(person, speaker, clock() + Brain.CONFIRM_MS)
        return Reply(if (speaker) Say.CONFIRM_SPEAKER_CALL else Say.CONFIRM_CALL, listOf(person.name)) to true
    }

    fun answer(heard: String): Pair<Reply, Boolean>? {
        permission = null
        val request = confirmation ?: return null
        confirmation = null
        if (clock() > request.until) return null
        return when (Confirmation.callAnswer(heard)) {
            true -> place(request)
            false -> Reply(Say.CANCELLED) to true
            null -> null
        }
    }

    fun finishPermission(granted: Boolean): Pair<Reply, Boolean>? {
        val request = permission ?: return null
        permission = null
        if (clock() > request.until) return null
        return if (granted) place(request, requestPermission = false) else dial(request.person)
    }

    fun dial(person: Person): Pair<Reply, Boolean> =
        if (hands.dial(person.number)) Reply(Say.CALLING, listOf(person.name)) to true
        else Reply(Say.FAILED, listOf(Catalog.byId("telefono").name)) to false

    private fun place(request: Request, requestPermission: Boolean = true): Pair<Reply, Boolean> =
        try {
            if (hands.placeCall(request.person.number, request.speaker)) Reply(Say.CALL_STARTED, listOf(request.person.name)) to true
            else dial(request.person)
        } catch (_: Hands.NoCallPermission) {
            if (requestPermission) {
                permission = request.copy(until = clock() + Brain.CONFIRM_MS)
                Reply(Say.NEED_CALL_PERMISSION) to false
            } else dial(request.person)
        }
}
