package com.historiasvivas.engine.turn
import com.historiasvivas.engine.model.*
data class WorldDelta(val event: StoryEvent,val factsToAdd:Set<String> = emptySet(),val consequencesToSchedule:List<Consequence> = emptyList(),val consequencesToResolve:Set<String> = emptySet())
data class TurnResult(val story:Story,val prose:String,val delta:WorldDelta)
