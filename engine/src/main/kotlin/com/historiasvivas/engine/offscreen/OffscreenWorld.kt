package com.historiasvivas.engine.offscreen
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.agency.AgencyIntent
import com.historiasvivas.engine.discovery.Evidence
import com.historiasvivas.engine.social.HeardClaim
import com.historiasvivas.engine.social.InformationChannel
import com.historiasvivas.engine.social.Testimony
import com.historiasvivas.engine.social.TestimonyIntent

@Serializable enum class DiscoveryChannel { WITNESSED, TOLD, RUMOR, EVIDENCE, INVESTIGATION }
@Serializable data class HiddenEvent(
 val id:String,
 val turn:Long,
 val actorId:String,
 val summary:String,
 val knownBy:Set<String> = setOf(actorId),
 val discoverable:Boolean=true,
 val discoveredBy:Set<String> = emptySet()
)
data class OffscreenAction(val intent:AgencyIntent,val hiddenEvent:HiddenEvent)
data class Discovery(
 val eventId:String,
 val characterId:String,
 val channel:DiscoveryChannel,
 val interpretation:String,
 val sourceId:String?=null
)
data class DiscoverySources(
 val evidence:Map<String,Evidence> = emptyMap(),
 val testimonies:Map<String,Testimony> = emptyMap(),
 val heardClaims:Map<String,HeardClaim> = emptyMap()
)

class KnowledgeGate {
 fun visible(events:List<HiddenEvent>,characterId:String):List<HiddenEvent> =
  events.filter{characterId in it.knownBy || characterId in it.discoveredBy}
 fun hasGroundedKnowledge(
  event:HiddenEvent,
  characterId:String,
  sources:DiscoverySources
 ):Boolean{
  if(characterId in event.knownBy||characterId in event.discoveredBy)return true
  if(sources.evidence.values.any{
    it.discoveredBy==characterId&&it.relatedEventId==event.id
   })return true
  return sources.testimonies.values.any{testimony->
   testimony.channel!=InformationChannel.RUMOR&&
    testimony.intent==TestimonyIntent.HONEST&&
    testimony.relatedEventId==event.id&&
    (testimony.speakerId in event.knownBy||testimony.speakerId in event.discoveredBy)&&
    wasReceived(testimony,characterId,event.id,sources)
  }
 }
 fun discover(
  events:List<HiddenEvent>,
  discovery:Discovery,
  sources:DiscoverySources=DiscoverySources()
 ):List<HiddenEvent>{
  val target=events.firstOrNull{it.id==discovery.eventId}?:return events
  if(!target.discoverable||!isDiscoveryGrounded(target,discovery,sources))return events
  return events.map{event->
   if(event.id==target.id)
    event.copy(discoveredBy=event.discoveredBy+discovery.characterId)
   else event
  }
 }
 private fun isDiscoveryGrounded(
  event:HiddenEvent,
  discovery:Discovery,
  sources:DiscoverySources
 ):Boolean{
  if(discovery.interpretation.isBlank())return false
  val sourceId=discovery.sourceId
  return when(discovery.channel){
   DiscoveryChannel.WITNESSED->
    sourceId.isNullOrBlank()&&discovery.characterId in event.knownBy
   DiscoveryChannel.TOLD->sourceId?.let{sources.testimonies[it]}?.let{testimony->
    testimony.channel!=InformationChannel.RUMOR &&
     testimony.intent==TestimonyIntent.HONEST &&
     testimony.relatedEventId==event.id &&
     (testimony.speakerId in event.knownBy||testimony.speakerId in event.discoveredBy) &&
     wasReceived(testimony,discovery.characterId,event.id,sources)
   }?:false
   DiscoveryChannel.RUMOR->false
   DiscoveryChannel.EVIDENCE,DiscoveryChannel.INVESTIGATION->sourceId?.let{sources.evidence[it]}?.let{
    it.discoveredBy==discovery.characterId && it.relatedEventId==event.id
   }?:false
  }
 }
 private fun wasReceived(
  testimony:Testimony,
  listenerId:String,
  eventId:String,
  sources:DiscoverySources
 ):Boolean = sources.heardClaims.values.any{
   it.testimonyId==testimony.id &&
    it.listenerId==listenerId &&
    it.speakerId==testimony.speakerId &&
    it.claim==testimony.claim &&
    it.channel==testimony.channel &&
    it.turn==testimony.turn &&
    (it.subject==null||it.subject==testimony.subject) &&
    it.relatedEventId==eventId
  }
}
