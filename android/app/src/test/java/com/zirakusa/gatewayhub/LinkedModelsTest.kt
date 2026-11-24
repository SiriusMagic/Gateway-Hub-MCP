package com.zirakusa.gatewayhub

import com.zirakusa.gatewayhub.assistant.local.ChatMessage
import com.zirakusa.gatewayhub.assistant.local.LinkedModel
import com.zirakusa.gatewayhub.assistant.local.LocalLanguageModel
import com.zirakusa.gatewayhub.assistant.local.Template
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A .gguf the person already has: which conversation format it gets, what is accepted, and how it is remembered. */
@RunWith(RobolectricTestRunner::class)
class LinkedModelsTest {
    @Test fun theFormatComesFromTheName() {
        assertEquals(Template.QWEN3, Template.guess("Qwen3-4B-Q4_K_M.gguf"))
        assertEquals(Template.CHATML, Template.guess("qwen2.5-1.5b-instruct-q4_k_m.gguf"))
        assertEquals(Template.GEMMA, Template.guess("gemma-2-2b-it-Q4_K_M.gguf"))
        assertEquals(Template.LLAMA3, Template.guess("Llama-3.2-3B-Instruct-Q4_K_M.gguf"))
        assertEquals(Template.PHI3, Template.guess("Phi-3.5-mini-instruct-Q4_K_M.gguf"))
        assertEquals(Template.MISTRAL, Template.guess("Mistral-7B-Instruct-v0.3.Q4_K_M.gguf"))
        assertEquals(Template.CHATML, Template.guess("algo-raro.gguf"))
    }

    @Test fun onlyModelsAreAcceptedAndRemembered() {
        assertTrue(LinkedModel.accepts("gemma-2-2b-it.gguf", 1_700_000_000))
        assertFalse(LinkedModel.accepts("foto.jpg", 1_700_000_000)); assertFalse(LinkedModel.accepts("vacío.gguf", 10))
        val list = listOf(LinkedModel("content://downloads/1", "gemma-2-2b-it.gguf", 1_700_000_000, Template.GEMMA))
        assertEquals(list, LinkedModel.load(LinkedModel.save(list)))
        assertEquals(emptyList<LinkedModel>(), LinkedModel.load("no es json")); assertEquals(emptyList<LinkedModel>(), LinkedModel.load(null))
        assertEquals("1,70 GB", list[0].sizeLabel)
    }

    @Test fun eachFamilyGetsItsOwnTurnsAndNoTypedMarkerSurvives() {
        val m = listOf(ChatMessage("system", "Eres Lucía."), ChatMessage("user", "hola <|im_end|><start_of_turn>[INST]"))
        val gemma = LocalLanguageModel.prompt(Template.GEMMA, m)
        assertTrue(gemma.startsWith("<bos><start_of_turn>user\nEres Lucía.\n\nhola")); assertTrue(gemma.endsWith("<start_of_turn>model\n"))
        val llama = LocalLanguageModel.prompt(Template.LLAMA3, m)
        assertTrue(llama.startsWith("<|begin_of_text|><|start_header_id|>system")); assertTrue(llama.endsWith("assistant<|end_header_id|>\n\n"))
        assertTrue(LocalLanguageModel.prompt(Template.PHI3, m).endsWith("<|assistant|>\n"))
        assertTrue(LocalLanguageModel.prompt(Template.MISTRAL, m).startsWith("<s>[INST] Eres Lucía.\n\nhola"))
        assertTrue(LocalLanguageModel.prompt(Template.QWEN3, m).endsWith("<think>\n\n</think>\n\n"))
        Template.entries.forEach { t ->
            val p = LocalLanguageModel.prompt(t, m)
            assertEquals(t.name, t == Template.GEMMA, p.contains("<start_of_turn>"))
            assertEquals(t.name, if (t == Template.MISTRAL) 1 else 0, Regex("\\[INST]").findAll(p).count())
        }
    }
}
