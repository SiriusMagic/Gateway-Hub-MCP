package com.zirakusa.gatewayhub.assistant.local

import android.os.SystemClock
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Command
import com.zirakusa.gatewayhub.assistant.Reply
import com.zirakusa.gatewayhub.assistant.Say
import com.zirakusa.gatewayhub.assistant.Turn

internal class LocalTurns(private val message: (Int) -> String, private val complete: (Turn) -> Unit) {
    private var reviewTurn: Turn? = null

    fun route(assistant: LocalAssistant, heard: String, byVoice: Boolean): Boolean {
        val chatQuery = chatQuery(heard)
        if (chatQuery != null) {
            if (!assistant.models.hasModel) {
                complete(generated(heard, byVoice, Action.CHAT, SystemClock.elapsedRealtime(),
                    message(R.string.local_chat_model_first), false))
            } else chat(assistant, heard, chatQuery, byVoice)
            return true
        }
        StoneRequests.explicit(heard)?.let { query ->
            stone(assistant, heard, query, byVoice)
            return true
        }
        val hubQuery = hubQuery(heard) ?: return false
        runHub(assistant, heard, hubQuery, byVoice)
        return true
    }

    fun chat(assistant: LocalAssistant, heard: String, query: String, byVoice: Boolean) {
        val start = SystemClock.elapsedRealtime()
        assistant.work(onFailure = { complete(generated(heard, byVoice, Action.CHAT, start, message(it), false)) }) {
            complete(generated(heard, byVoice, Action.CHAT, start, assistant.chat(query)))
        }
    }

    /**
     * What the model makes of [heard], by the chosen [interpreter]: Lucía's orders ([run]), one family MCP tool (a card
     * first if it changes something), or, when nothing fits, a conversation.
     */
    fun interpret(assistant: LocalAssistant, heard: String, byVoice: Boolean, interpreter: com.zirakusa.gatewayhub.assistant.Interpreter,
        understood: (String) -> Boolean, run: (List<String>) -> Unit) {
        val start = SystemClock.elapsedRealtime()
        fun result(text: String, ok: Boolean = true) = generated(heard, byVoice, Action.PHONE_MCP, start, text, ok)
        assistant.work(onFailure = { complete(generated(heard, byVoice, Action.CHAT, start, message(it), false)) }) {
            if (interpreter != com.zirakusa.gatewayhub.assistant.Interpreter.MCP) {
                val orders = assistant.orders(heard, understood)
                if (orders.isNotEmpty()) return@work run(orders)
            }
            if (interpreter != com.zirakusa.gatewayhub.assistant.Interpreter.FEELINGS) {
                val call = assistant.mcpPlan(heard)
                if (call != null) {
                    if (call.readOnly) complete(result(assistant.execute(call).ifBlank { message(R.string.local_hub_done) }))
                    else { reviewTurn = result(message(R.string.local_hub_review), false); assistant.review(call) }
                    return@work
                }
            }
            complete(generated(heard, byVoice, Action.CHAT, start, assistant.chat(heard)))
        }
    }

    fun stone(assistant: LocalAssistant, heard: String, query: String, byVoice: Boolean) {
        val start = SystemClock.elapsedRealtime()
        fun finish(text: String, ok: Boolean = false) = complete(generated(heard, byVoice, Action.PHONE_MCP, start, text, ok))
        if (!assistant.models.hasModel) {
            finish(message(R.string.local_chat_model_first))
            return
        }
        assistant.work(onFailure = { finish(message(it)) }) {
            val answer = assistant.askStone(query)
            finish(answer ?: message(R.string.phone_mcp_no_tool), answer != null)
        }
    }

    private fun runHub(assistant: LocalAssistant, heard: String, request: String, byVoice: Boolean) {
        val start = SystemClock.elapsedRealtime()
        fun result(text: String, ok: Boolean = false) = generated(heard, byVoice, Action.HUB, start, text, ok)
        fun fail(id: Int) = complete(result(message(id)))
        when {
            assistant.hub.link == null -> fail(R.string.local_hub_connect_first)
            !assistant.models.hasModel -> fail(R.string.local_hub_model_first)
            else -> assistant.work(onFailure = ::fail) {
                val call = assistant.decide(request)
                when {
                    call == null -> fail(R.string.local_hub_no_tool)
                    call.readOnly -> complete(result(assistant.execute(call).ifBlank { message(R.string.local_hub_done) }, true))
                    else -> {
                        reviewTurn = result(message(R.string.local_hub_review))
                        assistant.review(call)
                    }
                }
            }
        }
    }

    fun confirm(assistant: LocalAssistant, confirmed: Boolean) {
        val turn = reviewTurn ?: return
        val call = assistant.dismiss() ?: return
        reviewTurn = null
        fun finish(text: String, ok: Boolean = false) = complete(turn.copy(reply = Reply(Say.GENERATED, listOf(text)), ok = ok))
        if (!confirmed) return finish(message(R.string.local_hub_cancelled))
        assistant.work(onFailure = { finish(message(it)) }, cancellable = false) {
            finish(assistant.execute(call).ifBlank { message(R.string.local_hub_done) }, true)
        }
    }

    private fun generated(heard: String, byVoice: Boolean, action: Action, start: Long, text: String, ok: Boolean = true) =
        Turn(heard, byVoice, Command(action), null, Reply(Say.GENERATED, listOf(text)), ok, SystemClock.elapsedRealtime() - start)

    companion object {
        private const val HUB = "(?:(?:gateway )?hub|portal|jab)"
        private val prefix = Regex("^(?:en (?:el )?$HUB|gateway hub|hub|mcp|eme ce pe)[:,]?\\s+", RegexOption.IGNORE_CASE)
        private val suffix = Regex("\\s+(?:en|con|del)\\s+(?:el\\s+)?$HUB[.!?¿]*$", RegexOption.IGNORE_CASE)
        private val chatPrefix = Regex("^charla[:,]?\\s+", RegexOption.IGNORE_CASE)

        internal fun hubQuery(text: String): String? {
            val trimmed = text.trim().trimStart('¿', '¡')
            val query = trimmed.replaceFirst(prefix, "").replaceFirst(suffix, "").trim()
            return query.takeIf { it.isNotBlank() && it != trimmed }
        }

        internal fun chatQuery(text: String): String? {
            val query = text.replaceFirst(chatPrefix, "").trim()
            return query.takeIf { it.isNotBlank() && it != text }
        }
    }
}
