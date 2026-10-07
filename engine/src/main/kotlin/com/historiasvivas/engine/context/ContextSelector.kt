package com.historiasvivas.engine.context
import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.narrative.*
import com.historiasvivas.engine.memory.*
import com.historiasvivas.engine.character.EmotionalTrace
import com.historiasvivas.engine.spatial.SpatialEngine

data class NarrativeContext(
 val turn:Long,
 val premise:String,
 val recentEvents:List<StoryEvent>,
 val relevantMemories:List<RelevantMemory>,
 val subjectiveMemories:Map<String,List<EmotionalTrace>>,
 val canonicalEvents:List<StoryEvent>,
 val relevantCharacters:List<Character>,
 val dueConsequences:List<Consequence>,
 val knownFacts:Set<String>,
 val autonomousIntents:List<AgencyIntent> = emptyList(),
 val narrativeBeat:NarrativeBeat
)
class ContextSelector(
 private val recentLimit:Int=8,
 private val agencySelector:AgencySelector=AgencySelector(),
 private val weightEngine:NarrativeWeightEngine=NarrativeWeightEngine(),
 private val relevanceEngine:MemoryRelevanceEngine=MemoryRelevanceEngine(),
 private val spatialEngine:SpatialEngine=SpatialEngine()
){
 fun select(story:Story,userAction:String):NarrativeContext{
  val terms=userAction.lowercase().split(Regex("\\W+")).filter{it.length>=3}.toSet()
  val protagonistLocation=spatialEngine.locationOf(story.world.spatial,story.protagonistId)
  val presentIds=protagonistLocation?.let{spatialEngine.presentAt(story.world.spatial,it)}?:emptySet()
  val chars=story.world.characters.values.filter{c->
   c.id in presentIds || terms.any{t->c.name.lowercase().contains(t)||c.id.lowercase()==t}
  }
  val due=story.world.pendingConsequences.filter{!it.resolved&&it.earliestTurn<=story.turn+1}
  val relevant=relevanceEngine.select(story,userAction)
  val relevantIds=relevant.map{it.event.id}.toSet()
  val subjective=story.world.characters.values.associate{character->character.id to character.mind.emotionalMemory.values.filter{it.eventId in relevantIds}.sortedByDescending{it.salience}.take(4)}.filterValues{it.isNotEmpty()}
  return NarrativeContext(
   story.turn,story.premise,story.memory.recentEvents.takeLast(recentLimit),
   relevant,subjective,story.memory.canonicalEvents,chars,due,story.world.facts,
   agencySelector.select(story),weightEngine.assess(story,userAction)
  )
 }
}
