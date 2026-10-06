package com.historiasvivas.engine.ai
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.context.NarrativeContext
import com.historiasvivas.engine.model.*

data class NarrativeRequest(val story:Story,val userAction:String,val context:NarrativeContext)
data class NarrativeResponse(
 val prose:String,
 val eventSummary:String,
 val participantIds:Set<String> = emptySet(),
 val tags:Set<String> = emptySet(),
 val canonical:Boolean = false,
 val proposedFacts:Set<String> = emptySet(),
 val proposedConsequences:List<Consequence> = emptyList(),
 val resolvedConsequenceIds:Set<String> = emptySet(),
 val proposedMindDeltas:List<MindDelta> = emptyList(),
 val proposedPerceptionDeltas:List<PerceptionDelta> = emptyList()
)
interface NarrativeAI { suspend fun continueStory(request:NarrativeRequest):NarrativeResponse }
