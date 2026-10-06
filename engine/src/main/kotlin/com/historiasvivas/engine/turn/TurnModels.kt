package com.historiasvivas.engine.turn
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
data class WorldDelta(
 val event:StoryEvent,
 val factsToAdd:Set<String> = emptySet(),
 val consequencesToSchedule:List<Consequence> = emptyList(),
 val consequencesToResolve:Set<String> = emptySet(),
 val mindDeltas:List<MindDelta> = emptyList(),
 val perceptionDeltas:List<PerceptionDelta> = emptyList()
)
data class TurnResult(val story:Story,val prose:String,val delta:WorldDelta)
