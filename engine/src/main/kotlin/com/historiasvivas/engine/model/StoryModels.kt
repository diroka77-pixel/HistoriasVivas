package com.historiasvivas.engine.model

import kotlinx.serialization.Serializable
import com.historiasvivas.engine.character.CharacterMind
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.world.EnvironmentState

@Serializable enum class StoryStatus { ACTIVE, ENDING, FINISHED }
@Serializable enum class FactVisibility { PUBLIC, LIMITED, HIDDEN }
@Serializable data class WorldFact(
 val id:String,
 val text:String,
 val visibility:FactVisibility=FactVisibility.PUBLIC,
 val knownBy:Set<String> = emptySet()
){
 fun visibleTo(characterId:String)=visibility==FactVisibility.PUBLIC || characterId in knownBy
}

@Serializable data class Story(
 val id:String,val title:String,val premise:String,
 val world:WorldState=WorldState(),val memory:StoryMemory=StoryMemory(),
 val status:StoryStatus=StoryStatus.ACTIVE,val turn:Long=0
)
@Serializable data class WorldState(
 val characters:Map<String,Character> = emptyMap(),
 val locations:Map<String,Location> = emptyMap(),
 val relationships:List<Relationship> = emptyList(),
 val secrets:List<Secret> = emptyList(),
 val pendingConsequences:List<Consequence> = emptyList(),
 val facts:Set<String> = emptySet(),
 val scopedFacts:List<WorldFact> = emptyList(),
 val hiddenEvents:List<HiddenEvent> = emptyList(),
 val environment:EnvironmentState=EnvironmentState(),
 val worldClock:Long=0
)
@Serializable data class Character(val id:String,val name:String,val traits:Set<String> = emptySet(),val goals:List<String> = emptyList(),val knowledge:Set<String> = emptySet(),val mind:CharacterMind=CharacterMind(),val alive:Boolean=true)
@Serializable data class Location(val id:String,val name:String,val facts:Set<String> = emptySet())
@Serializable data class Relationship(val fromCharacterId:String,val toCharacterId:String,val trust:Int=0,val affection:Int=0,val fear:Int=0,val notes:Set<String> = emptySet())
@Serializable data class Secret(val id:String,val truth:String,val knownBy:Set<String> = emptySet(),val revealed:Boolean=false)
@Serializable data class StoryEvent(val id:String,val turn:Long,val summary:String,val participants:Set<String> = emptySet(),val tags:Set<String> = emptySet(),val permanent:Boolean=false)
@Serializable data class Consequence(val id:String,val causeEventId:String,val description:String,val earliestTurn:Long,val resolved:Boolean=false)
@Serializable data class StoryMemory(val recentEvents:List<StoryEvent> = emptyList(),val canonicalEvents:List<StoryEvent> = emptyList(),val longTermSummary:String="")
