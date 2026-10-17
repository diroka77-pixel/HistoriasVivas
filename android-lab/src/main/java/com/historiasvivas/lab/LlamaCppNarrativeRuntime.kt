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
    private val recentResponses = ArrayDeque<String>()
    override suspend fun generate(
        packet: NarrativePacket,
        userAction: String,
        config: LocalGenerationConfig
    ): NarrativeResponse {
        val firstPrompt = promptBuilder.build(packet, userAction)
        var prose = cleanGeneratedText(generateText(firstPrompt, config.maxOutputTokens))

        if (isEchoOrTooShort(prose, userAction) || wasRecentlyUsed(prose)) {
            prose = cleanGeneratedText(
                generateText(
                    promptBuilder.build(packet, userAction) + "\n\nATENCIÓN: la respuesta anterior fue repetitiva o insuficiente. Genera una continuación DIFERENTE, basada en esta acción y los hechos actuales. No reutilices frases anteriores.",
                    config.maxOutputTokens
                )
            )
        }

        if (isEchoOrTooShort(prose, userAction) || wasRecentlyUsed(prose)) {
            throw IllegalStateException("El narrador no ha generado una continuación válida. Vuelve a intentarlo; el turno no se guardará.")
        }
        recentResponses.addLast(normalize(prose))
        while (recentResponses.size > 8) recentResponses.removeFirst()

        return NarrativeResponse(
            prose = prose,
            eventSummary = "Daniel actuó: " + userAction.take(180),
            participantIds = setOf(packet.player.protagonistId),
            tags = setOf("local-narrative")
        )
    }

    private suspend fun generateText(prompt: String, maxTokens: Int): String =
        engine.sendUserPrompt(prompt, maxTokens)
            .toList()
            .joinToString("")

    private fun cleanGeneratedText(value: String): String {
        val withoutThinking = value
            .replace(Regex("(?s)<think>.*?</think>"), "")
            .trim()
        if (withoutThinking.isBlank()) return ""

        val sentences = withoutThinking.split(Regex("(?<=[.!?…])\\s+"))
        val accepted = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (sentence in sentences) {
            val cleanSentence = sentence.trim()
            val normalized = normalize(cleanSentence)
            val wordCount = normalized.split(' ').count { it.isNotBlank() }

            if (wordCount >= 6 && !seen.add(normalized)) break
            if (cleanSentence.isNotBlank()) accepted += cleanSentence
        }

        return accepted.joinToString(" ").trim()
    }

    private fun wasRecentlyUsed(prose: String): Boolean {
        val candidate = normalize(prose)
        if (candidate.isBlank()) return true
        return recentResponses.any { previous ->
            candidate == previous ||
                (candidate.length > 90 && previous.length > 90 &&
                    (candidate.contains(previous) || previous.contains(candidate)))
        }
    }

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
