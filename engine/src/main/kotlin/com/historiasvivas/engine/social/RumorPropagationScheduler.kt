package com.historiasvivas.engine.social

import com.historiasvivas.engine.model.Character
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.spatial.SpatialEngine

data class RumorDelivery(
 val testimonyId:String,
 val speakerId:String,
 val listenerId:String,
 val confidence:Int,
 val relatedEventId:String?
)

data class RumorPropagationCycle(
 val deliveries:List<RumorDelivery> = emptyList()
)

class RumorPropagationScheduler(
 private val rumors:RumorEngine=RumorEngine(),
 private val social:SocialInformationEngine=SocialInformationEngine(),
 private val spatial:SpatialEngine=SpatialEngine()
){
 fun run(story:Story,maxRelays:Int=1):Pair<Story,RumorPropagationCycle>{
  if(maxRelays<=0)return story to RumorPropagationCycle()
  var current=story
  val deliveries=mutableListOf<RumorDelivery>()

  story.world.socialKnowledge.heardClaims.values.asSequence()
   .filter{it.turn<story.turn&&it.confidence>=25}
   .sortedWith(compareByDescending<HeardClaim>{it.confidence}.thenBy{it.testimonyId}.thenBy{it.listenerId})
   .forEach{original->
    if(deliveries.size>=maxRelays)return@forEach
    val speaker=current.world.characters[original.listenerId]?.takeIf{it.alive}?:return@forEach
    if(propagationScore(speaker)<30)return@forEach
    if(current.world.socialKnowledge.testimonies.values.any{
      it.channel==InformationChannel.RUMOR&&
       it.speakerId==speaker.id&&
       original.testimonyId in it.sourceEvidenceIds
     })return@forEach
    val location=spatial.locationOf(current.world.spatial,speaker.id)?:return@forEach
    val listener=spatial.presentAt(current.world.spatial,location).asSequence()
     .filter{it!=speaker.id&&it!=original.speakerId}
     .mapNotNull{current.world.characters[it]}
     .filter{it.alive&&!alreadyKnows(current,it.id,original)}
     .sortedWith(
      compareByDescending<Character>{it.id==current.protagonistId}
       .thenByDescending{candidate->
        current.world.relationships.firstOrNull{
         it.fromCharacterId==speaker.id&&it.toCharacterId==candidate.id
        }?.trust?:0
       }
       .thenBy{it.id}
     ).firstOrNull()?:return@forEach

    val testimony=rumors.relay(original,speaker.id,listener.id,current.turn)
    val heard=social.hear(testimony,listener.id,listener.mind)
    val updatedListener=listener.copy(mind=listener.mind.copy(
     beliefs=listener.mind.beliefs+social.asBelief(heard)
    ))
    val knowledge=current.world.socialKnowledge.copy(
     testimonies=current.world.socialKnowledge.testimonies+(testimony.id to testimony),
     heardClaims=current.world.socialKnowledge.heardClaims+
      (testimony.id+":"+listener.id to heard)
    )
    current=current.copy(world=current.world.copy(
     characters=current.world.characters+(listener.id to updatedListener),
     socialKnowledge=knowledge
    ))
    deliveries+=RumorDelivery(
     testimony.id,speaker.id,listener.id,heard.confidence,heard.relatedEventId
    )
   }
  return current to RumorPropagationCycle(deliveries)
 }

 private fun propagationScore(character:Character):Int{
  val p=character.mind.personality.dimensions
  val honesty=p["honesty"]?:0
  val compassion=p["compassion"]?:0
  val loyalty=p["loyalty"]?:0
  val sociability=p["sociability"]?:0
  val secrecy=p["secrecy"]?:0
  val caution=p["caution"]?:0
  return honesty/3+compassion/2+loyalty/4+sociability-secrecy-caution/3
 }

 private fun alreadyKnows(story:Story,listenerId:String,original:HeardClaim):Boolean{
  val eventId=original.relatedEventId
  if(eventId!=null){
   val event=story.world.hiddenEvents.firstOrNull{it.id==eventId}
   if(event!=null&&(listenerId in event.knownBy||listenerId in event.discoveredBy))return true
   if(story.world.socialKnowledge.heardClaims.values.any{
     it.listenerId==listenerId&&it.relatedEventId==eventId
    })return true
  }
  return story.world.socialKnowledge.heardClaims.values.any{
   it.listenerId==listenerId&&it.claim==original.claim
  }
 }
}
