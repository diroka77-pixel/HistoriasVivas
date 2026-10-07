package com.historiasvivas.engine.turn
import com.historiasvivas.engine.character.CharacterMindReducer
import com.historiasvivas.engine.character.PersonalityGrowthEngine
import com.historiasvivas.engine.memory.MemoryEngine
import com.historiasvivas.engine.model.*

class WorldReducer(
 private val memoryEngine:MemoryEngine=MemoryEngine(),
 private val mindReducer:CharacterMindReducer=CharacterMindReducer(),
 private val growthEngine:PersonalityGrowthEngine=PersonalityGrowthEngine()
){
 fun apply(story:Story,delta:WorldDelta):Story{
  require(delta.event.turn==story.turn+1){"Event turn must advance exactly one turn"}
  val ids=story.world.pendingConsequences.map{it.id}.toSet()
  require(delta.consequencesToSchedule.none{it.id in ids}){"Duplicate consequence id"}
  val consequences=story.world.pendingConsequences.map{if(it.id in delta.consequencesToResolve)it.copy(resolved=true)else it}+delta.consequencesToSchedule
  val characters=story.world.characters.toMutableMap()
  delta.mindDeltas.forEach{d->characters[d.characterId]?.let{characters[d.characterId]=mindReducer.apply(it,d)}}
  delta.perceptionDeltas.forEach{d->characters[d.observerId]?.let{characters[d.observerId]=mindReducer.perceive(it,d)}}
  characters.keys.toList().forEach{id->characters[id]?.let{characters[id]=growthEngine.evolve(it)}}
  return story.copy(
   turn=story.turn+1,
   memory=memoryEngine.remember(story.memory,delta.event),
   world=story.world.copy(
    characters=characters,
    facts=story.world.facts+delta.factsToAdd,
    pendingConsequences=consequences,
    hiddenEvents=(story.world.hiddenEvents+delta.hiddenEventsToAdd).distinctBy{it.id},
    worldClock=story.world.worldClock+1
   )
  )
 }
}
