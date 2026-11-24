package com.zirakusa.gatewayhub.assistant

/**
 * Music must really sound before Lucía's «Reproduciendo» stands. Opening a player is not enough: an older Stone only
 * opens, and a «play» key sent before its player is ready is lost. So: if nothing sounds after [FIRST] ms, «play» is
 * pressed again (the player is up by then); if still nothing after [SECOND] ms more, [silent] tells the screen, which
 * corrects the answer and marks the order as failed. Pure: the phone gives [playing], [press] and [later].
 */
class PlayCheck(private val playing: () -> Boolean, private val press: () -> Unit,
    private val later: (Long, () -> Unit) -> Unit, private val silent: () -> Unit) {
    private var round = 0

    fun start() {
        val mine = ++round                                   // a newer order cancels the older check
        later(FIRST) {
            if (mine != round || playing()) return@later
            press()
            later(SECOND) { if (mine == round && !playing()) silent() }
        }
    }

    /** Pause or stop asked: silence is right, nothing to check. */
    fun cancel() { round++ }

    companion object { const val FIRST = 2_500L; const val SECOND = 3_500L }
}
