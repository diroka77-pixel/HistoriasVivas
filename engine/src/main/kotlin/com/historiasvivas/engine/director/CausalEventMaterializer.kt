package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.HiddenEvent

data class MaterializedEmergentEvent(
 val event:StoryEvent?=null,
 val hiddenEvent:HiddenEvent?=null,
 val consequences:List<Consequence> = emptyList(),
 val source:EmergentEventProposal
)

class CausalEventMaterializer {
 fun materialize(story:Story,proposal:EmergentEventProposal):MaterializedEmergentEvent?{
  if(proposal.score<65 || proposal.factors.size<2)return null
  return when(proposal.kind){
   EmergentEventKind.RESOURCE_CRISIS->{
    val threatened=story.world.environment.resources.values.filter{it.amount<=it.criticalBelow+10}
    if(threatened.isEmpty())null else {
     val event=StoryEvent(proposal.id,story.turn+1,proposal.summary,tags=setOf("emergent","resource"))
     val consequences=threatened.map{r->Consequence(proposal.id+"-"+r.id,event.id,"Shortage of "+r.id+" will worsen if nobody intervenes",story.turn+3)}
     MaterializedEmergentEvent(event=event,consequences=consequences,source=proposal)
    }
   }
   EmergentEventKind.SOCIAL_FRACTURE->{
    val relation=story.world.relationships.minByOrNull{it.trust}?:return null
    if(relation.trust>-35)return null
    val actor=story.world.characters[relation.fromCharacterId]?:return null
    if(story.world.characters[relation.toCharacterId]==null)return null
    val hidden=HiddenEvent(proposal.id,story.turn+1,actor.id,proposal.summary,knownBy=setOf(actor.id))
    MaterializedEmergentEvent(hiddenEvent=hidden,source=proposal)
   }
   EmergentEventKind.DISCOVERY_OPPORTUNITY->{
    val signals=story.world.discovery.signals.values.filter{it.persistent&&it.salience>=65}
    if(signals.size<2)null else {
     val participants=signals.flatMap{it.noticedBy}.toSet()
     val event=StoryEvent(proposal.id,story.turn+1,proposal.summary,participants=participants,tags=setOf("emergent","discovery"))
     MaterializedEmergentEvent(event=event,source=proposal)
    }
   }
   EmergentEventKind.EXPEDITION_DANGER->{
    val journey=story.world.spatial.journeys.values.sortedBy{it.characterId}.firstOrNull()?:return null
    val event=StoryEvent(proposal.id,story.turn+1,proposal.summary,participants=setOf(journey.characterId),tags=setOf("emergent","expedition","danger"))
    val consequence=Consequence(proposal.id+"-"+journey.characterId,event.id,"The expedition remains exposed to dangerous travel conditions",story.turn+2)
    MaterializedEmergentEvent(event=event,consequences=listOf(consequence),source=proposal)
   }
  }
 }
}