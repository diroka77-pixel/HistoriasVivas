package com.historiasvivas.engine.social

import com.historiasvivas.engine.model.Character
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.offscreen.Discovery
import com.historiasvivas.engine.offscreen.DiscoveryChannel
import com.historiasvivas.engine.offscreen.DiscoverySources
import com.historiasvivas.engine.offscreen.KnowledgeGate
import com.historiasvivas.engine.spatial.SpatialEngine

data class RelationshipDisclosureDelivery(
 val testimonyId:String,
 val speakerId:String,
 val listenerId:String,
 val eventId:String
)

data class RelationshipDisclosureCycle(
 val deliveries:List<RelationshipDisclosureDelivery> = emptyList()
)

class RelationshipRepairDisclosureScheduler(
 private val disclosure:RelationshipRepairDisclosure=RelationshipRepairDisclosure(),
 private val social:SocialInformationEngine=SocialInformationEngine(),
 private val knowledge:KnowledgeGate=KnowledgeGate(),
 private val spatial:SpatialEngine=SpatialEngine()
){
 fun run(story:Story,maxDisclosures:Int=1):Pair<Story,RelationshipDisclosureCycle>{
  if(maxDisclosures<=0)return story to RelationshipDisclosureCycle()
  var current=story
  val deliveries=mutableListOf<RelationshipDisclosureDelivery>()

  story.world.socialKnowledge.relationshipRepairs.values.asSequence()
   .filter{it.turn<story.turn}
   .sortedWith(compareBy<RelationshipRepairOutcome>{it.turn}.thenBy{it.attemptId})
   .forEach{outcome->
    if(deliveries.size>=maxDisclosures)return@forEach
    val eventId="hidden-"+outcome.attemptId
    if(current.world.socialKnowledge.testimonies.values.any{
      it.relatedEventId==eventId&&it.channel==InformationChannel.DIRECT_TESTIMONY
     })return@forEach
    val hidden=current.world.hiddenEvents.firstOrNull{it.id==eventId}?:return@forEach
    val speakers=listOf(outcome.offenderId,outcome.harmedId).mapNotNull{id->
     current.world.characters[id]?.takeIf{it.alive}?.let{it to disclosureScore(it,outcome)}
    }.filter{it.second>=50}
     .sortedWith(compareByDescending<Pair<Character,Int>>{it.second}.thenBy{it.first.id})
    val speaker=speakers.firstOrNull()?.first?:return@forEach
    val location=spatial.locationOf(current.world.spatial,speaker.id)?:return@forEach
    val participantIds=setOf(outcome.offenderId,outcome.harmedId)
    val listeners=spatial.presentAt(current.world.spatial,location).asSequence()
     .filter{it!=speaker.id&&it !in participantIds}
     .mapNotNull{current.world.characters[it]}
     .filter{it.alive&&it.id !in hidden.knownBy&&it.id !in hidden.discoveredBy}
     .sortedWith(
      compareByDescending<Character>{it.id==current.protagonistId}
       .thenByDescending{listener->
        current.world.relationships.firstOrNull{
         it.fromCharacterId==speaker.id&&it.toCharacterId==listener.id
        }?.trust?:0
       }
       .thenBy{it.id}
     ).toList()
    val listener=listeners.firstOrNull()?:return@forEach
    val disclosed=disclosure.disclose(current,outcome.attemptId,speaker.id,current.turn)?:return@forEach
    val heard=social.hear(disclosed.testimony,listener.id,listener.mind)
    val updatedListener=listener.copy(mind=listener.mind.copy(
     beliefs=listener.mind.beliefs+social.asBelief(heard)
    ))
    val socialKnowledge=disclosed.story.world.socialKnowledge.copy(
     heardClaims=disclosed.story.world.socialKnowledge.heardClaims+
      (disclosed.testimony.id+":"+listener.id to heard)
    )
    val sources=DiscoverySources(
     testimonies=socialKnowledge.testimonies,
     heardClaims=socialKnowledge.heardClaims
    )
    val discoveredEvents=knowledge.discover(
     disclosed.story.world.hiddenEvents,
     Discovery(
      eventId,
      listener.id,
      DiscoveryChannel.TOLD,
      disclosed.testimony.claim,
      disclosed.testimony.id
     ),
     sources
    )
    current=disclosed.story.copy(world=disclosed.story.world.copy(
     characters=disclosed.story.world.characters+(listener.id to updatedListener),
     socialKnowledge=socialKnowledge,
     hiddenEvents=discoveredEvents
    ))
    deliveries+=RelationshipDisclosureDelivery(
     disclosed.testimony.id,speaker.id,listener.id,eventId
    )
   }
  return current to RelationshipDisclosureCycle(deliveries)
 }

 private fun disclosureScore(character:Character,outcome:RelationshipRepairOutcome):Int{
  val p=character.mind.personality.dimensions
  val honesty=p["honesty"]?:0
  val loyalty=p["loyalty"]?:0
  val compassion=p["compassion"]?:0
  val secrecy=p["secrecy"]?:0
  val caution=p["caution"]?:0
  val pride=p["pride"]?:0
  val roleAdjustment=when{
   character.id==outcome.harmedId&&!outcome.accepted->15
   outcome.accepted->10
   else->0
  }
  return honesty+loyalty/2+compassion/3-secrecy-caution/3-pride/3+roleAdjustment
 }
}
