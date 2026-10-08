package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.BeliefRevision
import com.historiasvivas.engine.character.BeliefStatus
import com.historiasvivas.engine.model.Story

data class RumorBeliefDecay(
 val characterId:String,
 val sourceTestimonyId:String
)

data class RumorBeliefDecayCycle(
 val decayed:List<RumorBeliefDecay> = emptyList()
)

class RumorBeliefDecayEngine(
 private val unverifiedTurns:Long=4,
 private val confidenceLoss:Int=20
){
 fun apply(story:Story):Pair<Story,RumorBeliefDecayCycle>{
  var characters=story.world.characters
  val decayed=mutableListOf<RumorBeliefDecay>()

  story.world.characters.values.filter{it.alive}.forEach{character->
   val mind=characters.getValue(character.id).mind
   val candidates=mind.beliefs.mapNotNull{belief->
    val sourceId=belief.sourceEventId?:return@mapNotNull null
    val testimony=story.world.socialKnowledge.testimonies[sourceId]?:return@mapNotNull null
    if(testimony.channel!=InformationChannel.RUMOR||belief.status!=BeliefStatus.ACTIVE)return@mapNotNull null
    if(story.turn-testimony.turn<unverifiedTurns)return@mapNotNull null
    if(isGrounded(story,character.id,testimony.relatedEventId))return@mapNotNull null
    Triple(belief,sourceId,testimony)
   }
   if(candidates.isEmpty())return@forEach
   val sourceIds=candidates.map{it.second}.toSet()
   val revisedBeliefs=mind.beliefs.map{belief->
    if(belief.sourceEventId !in sourceIds)belief else belief.copy(
     confidence=(belief.confidence-confidenceLoss).coerceAtLeast(5),
     status=BeliefStatus.DOUBTED,
     lastRevisedTurn=story.turn
    )
   }
   val existing=mind.beliefRevisions.map{it.id}.toSet()
   val revisions=candidates.map{(previous,sourceId,_)->
    BeliefRevision(
     id="rumor-decay-"+character.id+"-"+sourceId+"-"+story.turn,
     previous=previous,
     revised=revisedBeliefs.first{it.sourceEventId==sourceId},
     confirmed=null,
     evidenceId=sourceId,
     sourceCharacterId=null,
     turn=story.turn
    )
   }.filter{it.id !in existing}
   characters=characters+(character.id to character.copy(mind=mind.copy(
    beliefs=revisedBeliefs,
    beliefRevisions=mind.beliefRevisions+revisions
   )))
   candidates.forEach{decayed+=RumorBeliefDecay(character.id,it.second)}
  }
  if(decayed.isEmpty())return story to RumorBeliefDecayCycle()
  return story.copy(world=story.world.copy(characters=characters)) to
   RumorBeliefDecayCycle(decayed)
 }

 private fun isGrounded(story:Story,characterId:String,eventId:String?):Boolean{
  if(eventId==null)return false
  val event=story.world.hiddenEvents.firstOrNull{it.id==eventId}
  if(event!=null&&(characterId in event.knownBy||characterId in event.discoveredBy))return true
  if(story.world.discovery.evidence.values.any{
    it.discoveredBy==characterId&&it.relatedEventId==eventId
   })return true
  return story.world.socialKnowledge.heardClaims.values.any{heard->
   heard.listenerId==characterId&&heard.relatedEventId==eventId&&
    heard.channel!=InformationChannel.RUMOR&&
    story.world.socialKnowledge.testimonies[heard.testimonyId]?.intent==TestimonyIntent.HONEST
  }
 }
}
