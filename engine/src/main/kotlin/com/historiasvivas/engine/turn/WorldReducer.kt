package com.historiasvivas.engine.turn
import com.historiasvivas.engine.character.CharacterMindReducer
import com.historiasvivas.engine.character.PersonalityGrowthEngine
import com.historiasvivas.engine.memory.MemoryEngine
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.world.EnvironmentSimulator
import com.historiasvivas.engine.spatial.SpatialEngine
import com.historiasvivas.engine.discovery.CausalDiscoveryGenerator

class WorldReducer(
 private val memoryEngine:MemoryEngine=MemoryEngine(),
 private val mindReducer:CharacterMindReducer=CharacterMindReducer(),
 private val growthEngine:PersonalityGrowthEngine=PersonalityGrowthEngine(),
 private val environmentSimulator:EnvironmentSimulator=EnvironmentSimulator(),
 private val spatialEngine:SpatialEngine=SpatialEngine(),
 private val discoveryGenerator:CausalDiscoveryGenerator=CausalDiscoveryGenerator()
){
 fun apply(story:Story,delta:WorldDelta):Story{
  require(delta.event.turn==story.turn+1){"Event turn must advance exactly one turn"}
  val ids=story.world.pendingConsequences.map{it.id}.toSet()
  require(delta.consequencesToSchedule.none{it.id in ids}){"Duplicate consequence id"}
  val environmentTick=environmentSimulator.tick(story)
  val advancedSpatial=spatialEngine.advanceJourneys(story.world.spatial,1,story.turn+1)
  val causalSnapshot=story.copy(turn=story.turn+1,world=story.world.copy(environment=environmentTick.next,spatial=advancedSpatial))
  val generatedSignals=discoveryGenerator.generate(causalSnapshot)
  val newSignals=generatedSignals.filterNot{it.id in story.world.discovery.signals}.associateBy{it.id}
  val discovery=story.world.discovery.copy(signals=story.world.discovery.signals+newSignals)
  val consequences=story.world.pendingConsequences.map{if(it.id in delta.consequencesToResolve)it.copy(resolved=true)else it}+delta.consequencesToSchedule
  val characters=story.world.characters.toMutableMap()
  delta.mindDeltas.forEach{d->characters[d.characterId]?.let{characters[d.characterId]=mindReducer.apply(it,d)}}
  delta.perceptionDeltas.forEach{d->characters[d.observerId]?.let{characters[d.observerId]=mindReducer.perceive(it,d)}}
  characters.keys.toList().forEach{id->characters[id]?.let{characters[id]=growthEngine.evolve(it)}}
  return story.copy(
   turn=story.turn+1,
   memory=(listOf(delta.event)+environmentTick.publicEvents+delta.worldEventsToRemember).fold(story.memory){m,e->memoryEngine.remember(m,e)},
   world=story.world.copy(
    characters=characters,
    facts=story.world.facts+delta.factsToAdd,
    pendingConsequences=consequences,
    hiddenEvents=(story.world.hiddenEvents+delta.hiddenEventsToAdd).distinctBy{it.id},
    environment=environmentTick.next,
    spatial=advancedSpatial,
    discovery=discovery,
    worldClock=story.world.worldClock+1
   )
  )
 }
}
