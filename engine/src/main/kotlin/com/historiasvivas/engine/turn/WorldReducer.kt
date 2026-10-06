package com.historiasvivas.engine.turn
import com.historiasvivas.engine.memory.MemoryEngine
import com.historiasvivas.engine.model.*
class WorldReducer(private val memoryEngine:MemoryEngine=MemoryEngine()){
 fun apply(story:Story,delta:WorldDelta):Story{
  require(delta.event.turn==story.turn+1){"Event turn must advance exactly one turn"}
  val ids=story.world.pendingConsequences.map{it.id}.toSet()
  require(delta.consequencesToSchedule.none{it.id in ids}){"Duplicate consequence id"}
  val consequences=story.world.pendingConsequences.map{if(it.id in delta.consequencesToResolve)it.copy(resolved=true)else it}+delta.consequencesToSchedule
  return story.copy(turn=story.turn+1,memory=memoryEngine.remember(story.memory,delta.event),world=story.world.copy(facts=story.world.facts+delta.factsToAdd,pendingConsequences=consequences,worldClock=story.world.worldClock+1))
 }
}