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
 fun discover(
  events:List<HiddenEvent>,
  discovery:Discovery,
  sources:DiscoverySources=DiscoverySources()
 ):List<HiddenEvent>{
  val target=events.firstOrNull{it.id==discovery.eventId}?:return events
  if(!target.discoverable||!isGrounded(target,discovery,sources))return events
  return events.map{event->
   if(event.id==target.id)
    event.copy(discoveredBy=event.discoveredBy+discovery.characterId)
   else event
  }
 }
 private fun isGrounded(
  event:HiddenEvent,
  discovery:Discovery,
  sources:DiscoverySources
 ):Boolean{
  if(discovery.interpretation.isBlank())return false
  val sourceId=discovery.sourceId
  return when(discovery.channel){
   DiscoveryChannel.WITNESSED->
    sourceId.isNullOrBlank()&&discovery.characterId in event.knownBy
   DiscoveryChannel.TOLD->sourceId?.let{sources.testimonies[it]}?.let{
    it.channel!=InformationChannel.RUMOR &&
     it.intent==TestimonyIntent.HONEST &&
     it.relatedEventId==event.id &&
     wasReceived(it,discovery,sources)
   }?:false
   DiscoveryChannel.RUMOR->false
   DiscoveryChannel.EVIDENCE,DiscoveryChannel.INVESTIGATION->sourceId?.let{sources.evidence[it]}?.let{
    it.discoveredBy==discovery.characterId && it.relatedEventId==event.id
   }?:false
  }
 }
 private fun wasReceived(testimony:Testimony,discovery:Discovery,sources:DiscoverySources):Boolean =
  sources.heardClaims.values.any{
   it.testimonyId==testimony.id &&
    it.listenerId==discovery.characterId &&
    it.relatedEventId==discovery.eventId
  }
}
