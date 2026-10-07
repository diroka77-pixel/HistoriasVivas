package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.*

enum class EmergentEventKind { RESOURCE_CRISIS, EXPEDITION_DANGER, SOCIAL_FRACTURE, DISCOVERY_OPPORTUNITY }
data class CausalFactor(val id:String,val weight:Int,val description:String)
data class EmergentEventProposal(val id:String,val kind:EmergentEventKind,val score:Int,val factors:List<CausalFactor>,val summary:String,val hidden:Boolean=true)

class CausalEventDirector(private val threshold:Int=65,private val cooldownTurns:Long=3){
 fun propose(story:Story,lastEmergentTurn:Long?=null):EmergentEventProposal?{
  if(lastEmergentTurn!=null && story.turn-lastEmergentTurn<cooldownTurns)return null
  val candidates=mutableListOf<EmergentEventProposal>()
  val low=story.world.environment.resources.values.filter{it.amount<=it.criticalBelow+10}
  val fauna=story.world.environment.pressures["fauna"]?.intensity?:0
  if(low.isNotEmpty()){
   val factors=mutableListOf(CausalFactor("resources",35,"Supplies are close to critical"))
   if(fauna>=50)factors+=CausalFactor("fauna",20,"Native fauna pressure is high")
   candidates+=EmergentEventProposal("resource-"+(story.turn+1),EmergentEventKind.RESOURCE_CRISIS,factors.sumOf{it.weight},factors,"Independent pressures threaten essential supplies",false)
  }
  val distrust=story.world.relationships.any{it.trust<=-35}
  val unresolved=story.world.characters.values.any{c->c.mind.emotionalMemory.values.count{it.unresolved&&it.salience>=70}>=2}
  if(distrust&&unresolved){
   val factors=listOf(CausalFactor("distrust",40,"Relationship deterioration"),CausalFactor("memory",30,"Strong unresolved experiences"))
   candidates+=EmergentEventProposal("social-"+(story.turn+1),EmergentEventKind.SOCIAL_FRACTURE,70,factors,"Accumulated distrust makes confrontation plausible")
  }
  return candidates.filter{it.score>=threshold}.maxByOrNull{it.score}
 }
}