package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.CharacterMindReducer
import com.historiasvivas.engine.character.PerceptionDelta
import com.historiasvivas.engine.discovery.SignalSense
import com.historiasvivas.engine.discovery.WorldSignal
import com.historiasvivas.engine.model.Relationship
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.spatial.SpatialEngine

data class RelationshipConsequenceCycle(
 val resolvedConsequenceIds:List<String> = emptyList(),
 val generatedSignalIds:List<String> = emptyList()
)

class RelationshipRepairConsequenceEngine(
 private val spatial:SpatialEngine=SpatialEngine(),
 private val minds:CharacterMindReducer=CharacterMindReducer()
){
 fun resolve(story:Story,maxConsequences:Int=2):Pair<Story,RelationshipConsequenceCycle>{
  if(maxConsequences<=0)return story to RelationshipConsequenceCycle()
  var current=story
  val resolved=mutableListOf<String>()
  val signals=mutableListOf<String>()

  story.world.pendingConsequences.asSequence()
   .filter{!it.resolved&&it.earliestTurn<=story.turn&&it.id.startsWith(PREFIX)}
   .sortedWith(compareBy<com.historiasvivas.engine.model.Consequence>{it.earliestTurn}.thenBy{it.id})
   .forEach{consequence->
    if(resolved.size>=maxConsequences)return@forEach
    val attemptId=consequence.id.removePrefix(PREFIX)
    val outcome=current.world.socialKnowledge.relationshipRepairs[attemptId]?:return@forEach
    val offender=current.world.characters[outcome.offenderId]?.takeIf{it.alive}?:return@forEach
    val harmed=current.world.characters[outcome.harmedId]?.takeIf{it.alive}?:return@forEach
    val offenderLocation=spatial.locationOf(current.world.spatial,offender.id)?:return@forEach
    if(spatial.locationOf(current.world.spatial,harmed.id)!=offenderLocation)return@forEach
    val hiddenEventId="hidden-"+attemptId
    if(current.world.hiddenEvents.none{it.id==hiddenEventId})return@forEach

    val trustChange=if(outcome.accepted)2 else -3
    val affectionChange=if(outcome.accepted)1 else -1
    val fearChange=if(outcome.accepted)-1 else 1
    val note=if(outcome.accepted)
     "Later conduct supports a cautious improvement with "+offender.id
    else
     "Later contact reinforces distance from "+offender.id
    val existing=current.world.relationships.firstOrNull{
     it.fromCharacterId==harmed.id&&it.toCharacterId==offender.id
    }?:Relationship(harmed.id,offender.id)
    val updatedRelationship=existing.copy(
     trust=(existing.trust+trustChange).coerceIn(-100,100),
     affection=(existing.affection+affectionChange).coerceIn(-100,100),
     fear=(existing.fear+fearChange).coerceIn(0,100),
     notes=existing.notes+note
    )
    val perceived=minds.perceive(
     harmed,
     PerceptionDelta(
      harmed.id,
      offender.id,
      impressionShifts=mapOf("trust" to trustChange),
      notesToAdd=setOf(note)
     )
    )
    val updatedHarmed=perceived.copy(mind=perceived.mind.copy(
     personalMemories=perceived.mind.personalMemories+note
    ))
    val signal=WorldSignal(
     id="repair-follow-through-"+attemptId,
     locationId=offenderLocation,
     sense=SignalSense.SIGHT,
     surface=if(outcome.accepted)
      offender.name+" and "+harmed.name+" coordinate carefully, checking each other's response."
     else
      offender.name+" and "+harmed.name+" keep a deliberate distance during the encounter.",
     hiddenCause=if(outcome.accepted)
      "Later behavior follows an accepted private repair."
     else
      "Later behavior follows a rejected private repair.",
     createdTurn=current.turn,
     salience=if(outcome.accepted)50 else 60,
     persistent=true,
     relatedEventId=hiddenEventId
    )
    val pending=current.world.pendingConsequences.map{
     if(it.id==consequence.id)it.copy(resolved=true) else it
    }
    current=current.copy(world=current.world.copy(
     characters=current.world.characters+(harmed.id to updatedHarmed),
     relationships=current.world.relationships.filterNot{
      it.fromCharacterId==harmed.id&&it.toCharacterId==offender.id
     }+updatedRelationship,
     pendingConsequences=pending,
     discovery=current.world.discovery.copy(
      signals=current.world.discovery.signals+(signal.id to signal)
     )
    ))
    resolved+=consequence.id
    signals+=signal.id
   }
  return current to RelationshipConsequenceCycle(resolved,signals)
 }

 companion object {
  private const val PREFIX="repair-consequence-"
 }
}
