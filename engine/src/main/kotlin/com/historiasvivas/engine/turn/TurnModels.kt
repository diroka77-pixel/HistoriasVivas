package com.historiasvivas.engine.turn
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.model.StoryEvent
data class WorldDelta(
 val event:StoryEvent,
 val factsToAdd:Set<String> = emptySet(),
 val consequencesToSchedule:List<Consequence> = emptyList(),
 val consequencesToResolve:Set<String> = emptySet(),
 val mindDeltas:List<MindDelta> = emptyList(),
 val perceptionDeltas:List<PerceptionDelta> = emptyList(),
 val hiddenEventsToAdd:List<HiddenEvent> = emptyList(),
 val worldEventsToRemember:List<StoryEvent> = emptyList()
)
data class TurnResult(val story:Story,val prose:String,val delta:WorldDelta)
