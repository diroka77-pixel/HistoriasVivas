package com.historiasvivas.engine

import com.historiasvivas.engine.ai.local.LocalNarrativePromptBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNarrativePromptBuilderTest {
    private val builder = LocalNarrativePromptBuilder()

    @Test fun systemPromptPreservesPlayerAgency() {
        val prompt = builder.systemPrompt()
        assertTrue(prompt.contains("No decidas la siguiente acción importante"))
        assertTrue(prompt.contains("Cada respuesta debe hacer avanzar la escena"))
    }

    @Test fun retryPromptUsesCurrentAction() {
        val action = "Dejo de hablar con el guardia y abro la puerta del sótano."
        val prompt = builder.retryAfterEcho(action)
        assertTrue(prompt.contains(action))
        assertTrue(prompt.contains("No repitas ninguna frase"))
        assertFalse(prompt.contains("Elige una opción"))
    }
}
