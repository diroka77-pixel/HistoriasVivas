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
   else -> null
  }
 }
}