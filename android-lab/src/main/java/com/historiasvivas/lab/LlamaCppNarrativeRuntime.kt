package com.historiasvivas.lab

import com.arm.aichat.InferenceEngine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.ai.local.*
import com.historiasvivas.engine.manuscript.StoryManuscript
import kotlinx.coroutines.flow.toList
import java.text.Normalizer
import java.util.Locale

class LlamaCppNarrativeRuntime(
    private val engine: InferenceEngine,
    private val promptBuilder: LocalNarrativePromptBuilder = LocalNarrativePromptBuilder()
) : LocalInferenceRuntime {
    private val recentResponses = ArrayDeque<String>()
    private var lastNarrativeExcerpt: String = ""
    fun restoreHistory(manuscript: StoryManuscript) {
        recentResponses.clear()
        manuscript.entries.takeLast(8).forEach { entry ->
            val normalized = normalize(entry.prose)
            if (normalized.isNotBlank()) recentResponses.addLast(normalized)
        }
        lastNarrativeExcerpt = manuscript.entries.lastOrNull()?.prose?.takeLast(900).orEmpty()
    }

    override suspend fun generate(
        packet: NarrativePacket,
        userAction: String,
        config: LocalGenerationConfig
    ): NarrativeResponse {
        val firstPrompt = promptBuilder.build(packet, userAction) +
            if (lastNarrativeExcerpt.isBlank()) "" else
                "\nÚLTIMA CONTINUACIÓN (solo para mantener continuidad, NO la repitas):\n" +
                    lastNarrativeExcerpt + "\n"
        var prose = cleanGeneratedText(generateText(firstPrompt, config.maxOutputTokens))

        if (isEchoOrTooShort(prose, userAction) || wasRecentlyUsed(prose)) {
            prose = cleanGeneratedText(
                generateText(
                    firstPrompt + "\n\n" + promptBuilder.retryAfterEcho(userAction) + "\nEvita este texto fallido: " + prose.take(350),
                    config.maxOutputTokens
                )
            )
        }

        if (isEchoOrTooShort(prose, userAction) || wasRecentlyUsed(prose)) {
            throw IllegalStateException("El narrador no ha generado una continuación válida. Vuelve a intentarlo; el turno no se guardará.")
        }
        lastNarrativeExcerpt = prose.takeLast(900)
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
                    (candidate.contains(previous) || previous.contains(candidate))) ||
                (candidate.length > 100 && previous.length > 100 &&
                    sharedWordSequence(candidate, previous, 16))
        }
    }

    private fun sharedWordSequence(a: String, b: String, window: Int): Boolean {
        val first = a.split(' ')
        val second = b.split(' ')
        if (first.size < window || second.size < window) return false
        val phrases = first.windowed(window).map { it.joinToString(" ") }.toHashSet()
        return second.windowed(window).any { it.joinToString(" ") in phrases }
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
