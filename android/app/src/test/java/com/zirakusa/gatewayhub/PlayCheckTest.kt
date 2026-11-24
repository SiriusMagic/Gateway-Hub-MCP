package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.PlayCheck
import org.junit.Assert.assertEquals
import org.junit.Test

/** «Reproduce música» must really sound: a second «play» when the player opened silent, and a correction if it stays so. */
class PlayCheckTest {
    private val waiting = mutableListOf<Pair<Long, () -> Unit>>()
    private var sounding = false; private var presses = 0; private var silent = 0
    private val check = PlayCheck({ sounding }, { presses++ }, { ms, run -> waiting += ms to run }, { silent++ })
    private fun tick() { val (_, run) = waiting.removeAt(0); run() }

    @Test fun musicThatStartsIsLeftAlone() {
        check.start(); sounding = true; tick()
        assertEquals(0, presses); assertEquals(0, silent); assertEquals(0, waiting.size)
    }

    @Test fun anOpenedButSilentPlayerGetsPlayPressed() {
        check.start(); assertEquals(PlayCheck.FIRST, waiting.single().first)
        tick(); assertEquals(1, presses)                         // the old Stone only opened: «play» now that it is up
        sounding = true; tick(); assertEquals(0, silent)
    }

    @Test fun stillSilentIsSaid() {
        check.start(); tick(); tick()
        assertEquals(1, presses); assertEquals(1, silent)
    }

    @Test fun aPauseOrANewerOrderCancelsTheCheck() {
        check.start(); check.cancel(); tick(); assertEquals(0, presses)
        check.start(); check.start(); tick(); assertEquals(0, presses)   // the first check gave way to the second
        tick(); tick(); assertEquals(1, presses); assertEquals(1, silent)
    }
}
