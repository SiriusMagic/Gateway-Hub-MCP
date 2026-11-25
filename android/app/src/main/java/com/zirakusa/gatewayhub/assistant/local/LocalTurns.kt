package com.zirakusa.gatewayhub.assistant.local

import android.os.SystemClock
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.assistant.Action
import com.zirakusa.gatewayhub.assistant.Command
import com.zirakusa.gatewayhub.assistant.Reply
import com.zirakusa.gatewayhub.assistant.Say
import com.zirakusa.gatewayhub.assistant.Turn

internal class LocalTurns(private val message: (Int) -> String, private val complete: (Turn) -> Unit) {
    fun routeChat(assistant: LocalAssistant, heard: String, byVoice: Boolean): Boolean {
        val query = chatQuery(heard) ?: return false
        val error = when {
            !assistant.chatEnabled -> R.string.local_chat_paused
            !assistant.models.hasModel -> R.string.local_chat_model_first
            else -> null
        }
        if (error != null) complete(generated(heard, byVoice, Action.CHAT, SystemClock.elapsedRealtime(), message(error), false))
        else chat(assistant, heard, query, byVoice)
        return true
    }

    fun chat(assistant: LocalAssistant, heard: String, query: String, byVoice: Boolean) {
        val start = SystemClock.elapsedRealtime()
        assistant.work(onFailure = { complete(generated(heard, byVoice, Action.CHAT, start, message(it), false)) }) {
            complete(generated(heard, byVoice, Action.CHAT, start, assistant.chat(query)))
        }
    }

    private fun generated(heard: String, byVoice: Boolean, action: Action, start: Long, text: String, ok: Boolean = true) =
        Turn(heard, byVoice, Command(action), null, Reply(Say.GENERATED, listOf(text)), ok, SystemClock.elapsedRealtime() - start)

    companion object {
        private const val HUB = "(?:(?:gateway )?hub|portal|jab)"
        private val prefix = Regex("^(?:en (?:el )?$HUB|gateway hub|hub|mcp|eme ce pe)[:,]?\\s+", RegexOption.IGNORE_CASE)
        private val suffix = Regex("\\s+(?:en|con|del)\\s+(?:el\\s+)?$HUB[.!?¿]*$", RegexOption.IGNORE_CASE)
        private val chatPrefix = Regex("^(?:charla|conversa|hablemos)[:,]?\\s+", RegexOption.IGNORE_CASE)

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
