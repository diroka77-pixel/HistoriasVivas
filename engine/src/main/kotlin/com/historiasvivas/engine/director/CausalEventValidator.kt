package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.*

data class CausalValidation(val accepted:Boolean,val reasons:List<String> = emptyList())

class CausalEventValidator {
 fun validate(story:Story,candidate:MaterializedEmergentEvent):CausalValidation{
  val errors=mutableListOf<String>()
  if(candidate.event==null && candidate.hiddenEvent==null)errors+="Event has no observable or hidden realization"
  candidate.hiddenEvent?.let{h->
   if(h.actorId !in story.world.characters)errors+="Unknown actor"
   if(!story.world.characters[h.actorId]!!.alive)errors+="Dead actor cannot initiate event"
  }
  candidate.consequences.forEach{c->
   if(c.earliestTurn<=story.turn)errors+="Consequence cannot predate its cause"
   if(story.world.pendingConsequences.any{it.id==c.id})errors+="Duplicate consequence"
  }
  val stillCausal=when(candidate.source.kind){
   EmergentEventKind.RESOURCE_CRISIS -> story.world.environment.resources.values.any{it.amount<=it.criticalBelow+10}
   EmergentEventKind.SOCIAL_FRACTURE -> story.world.relationships.any{it.trust<=-35}
   else -> true
  }
  if(!stillCausal)errors+="Original causal conditions no longer hold"
  return CausalValidation(errors.isEmpty(),errors)
 }
}