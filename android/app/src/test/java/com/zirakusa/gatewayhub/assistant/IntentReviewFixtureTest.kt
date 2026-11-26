package com.zirakusa.gatewayhub.assistant

import com.zirakusa.gatewayhub.FakeHands
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentReviewFixtureTest {
    @Test fun publicCases() {
        val brain = Brain(FakeHands())
        val fixtures = File("src/test/resources/intent-review").listFiles { file -> file.extension == "json" }
        require(!fixtures.isNullOrEmpty())
        for (file in fixtures.sortedBy { it.name }) {
            val label = "intent-fixture:${file.nameWithoutExtension}"
            val expected = JSONObject(file.readText())
            val phrase = expected.getString("phrase")
            val plan = IntentPlan.validated(phrase, Intents.split(phrase, brain::commandFor), brain::commandFor)
            val steps = plan?.steps.orEmpty()
            val calc = expected.string("calculation")
            assertEquals(label, if (calc != null) listOf(Action.CALCULATE, Action.PLAY_MUSIC) else listOf(Action.PLAY_MUSIC),
                steps.map { it.command.action })
            assertEquals(label, calc, steps.first().command["calculation"])
            val music = steps.last().command
            assertEquals(label, expected.string("query"), music["query"])
            assertEquals(label, expected.string("volume"), music["volume"])
            assertEquals(label, expected.optBoolean("equalizer"), music["equalizer"] == "1")
            assertEquals(label, expected.optBoolean("background"), music["background"] == "1")
        }
    }

    private fun JSONObject.string(key: String) = optString(key).takeUnless { isNull(key) }
}
