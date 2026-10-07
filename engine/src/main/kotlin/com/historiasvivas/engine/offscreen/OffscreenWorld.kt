package com.historiasvivas.engine.offscreen
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.agency.AgencyIntent

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

class KnowledgeGate {
 fun visible(events:List<HiddenEvent>,characterId:String):List<HiddenEvent> =
  events.filter{characterId in it.knownBy || characterId in it.discoveredBy}
 fun discover(events:List<HiddenEvent>,discovery:Discovery):List<HiddenEvent>{
  if(!isGrounded(discovery))return events
  return events.map{event->
   if(event.id==discovery.eventId && event.discoverable)
    event.copy(discoveredBy=event.discoveredBy+discovery.characterId)
   else event
  }
 }
 private fun isGrounded(discovery:Discovery):Boolean{
  if(discovery.interpretation.isBlank())return false
  return when(discovery.channel){
   DiscoveryChannel.TOLD,DiscoveryChannel.RUMOR->!discovery.sourceId.isNullOrBlank()
   DiscoveryChannel.WITNESSED,DiscoveryChannel.EVIDENCE,DiscoveryChannel.INVESTIGATION->true
  }
 }
}
