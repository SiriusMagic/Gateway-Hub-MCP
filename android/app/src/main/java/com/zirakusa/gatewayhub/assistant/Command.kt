package com.zirakusa.gatewayhub.assistant

/** What the user asked for. Each one is a node of the brain's graph. */
enum class Action {
    PLAY_MUSIC, PLAY_RECOMMENDED, MEDIA, ALARM, TIMER, CALL, MESSAGE, WEATHER, AGENDA, TODAY, TIME, DATE, PHOTO, SCAN, SPEED, COUNT, VOLUME, OPEN, UNKNOWN
}

/** Media buttons, for any player. */
/** [RESTART]: the song again from the start. */
enum class MediaKey { PAUSE, RESUME, NEXT, PREVIOUS, RESTART }

/** One understood order: the action and its pieces («hour», «minute», «seconds», «who», «text», «query», «app», «key», «title», «volume» 0–100 or «volume_step» up/down, said with any order). */
data class Command(val action: Action, val slots: Map<String, String> = emptyMap()) {
    operator fun get(key: String) = slots[key]
    fun int(key: String) = slots[key]?.toIntOrNull()
}
