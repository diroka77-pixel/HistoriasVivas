package com.historiasvivas.lab

import com.arm.aichat.InferenceEngine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.ai.local.*
import kotlinx.coroutines.flow.toList
import java.text.Normalizer
import java.util.Locale

class LlamaCppNarrativeRuntime(
    private val engine: InferenceEngine,
    private val promptBuilder: LocalNarrativePromptBuilder = LocalNarrativePromptBuilder()
) : LocalInferenceRuntime {
    override suspend fun generate(
        packet: NarrativePacket,
        userAction: String,
        config: LocalGenerationConfig
    ): NarrativeResponse {
        val firstPrompt = promptBuilder.build(packet, userAction)
        var prose = generateText(firstPrompt, config.maxOutputTokens)

        if (isEchoOrTooShort(prose, userAction)) {
            prose = generateText(
                promptBuilder.retryAfterEcho(userAction),
                config.maxOutputTokens
            )
        }

        return NarrativeResponse(
            prose = prose.ifBlank {
                "Durante unos segundos nadie responde. Entonces, un sonido profundo recorre las estructuras de Nadir y obliga a todos a mirar hacia las ruinas del norte. Algo acaba de cambiar."
            },
            eventSummary = "Daniel actuó: " + userAction.take(180),
            participantIds = setOf(packet.player.protagonistId),
            tags = setOf("local-narrative")
        )
    }

    private suspend fun generateText(prompt: String, maxTokens: Int): String =
        engine.sendUserPrompt(prompt, maxTokens)
            .toList()
            .joinToString("")
            .replace(Regex("(?s)<think>.*?</think>"), "")
            .trim()

    private fun isEchoOrTooShort(prose: String, userAction: String): Boolean {
        val output = normalize(prose)
        val input = normalize(userAction)
        if (output.split(' ').count { it.isNotBlank() } < 12) return true
        if (output == input) return true
        return output.length <= input.length + 35 &&
            (output.contains(input) || input.contains(output))
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9ñ ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
