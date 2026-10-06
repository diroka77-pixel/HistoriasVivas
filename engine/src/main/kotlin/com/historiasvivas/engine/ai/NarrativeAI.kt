package com.historiasvivas.engine.ai

import com.historiasvivas.engine.model.Story

data class NarrativeRequest(val story: Story, val userAction: String)
data class NarrativeResponse(val prose: String, val proposedFacts: Set<String> = emptySet())

interface NarrativeAI {
    suspend fun continueStory(request: NarrativeRequest): NarrativeResponse
}
