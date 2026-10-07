package com.historiasvivas.engine.discovery

import com.historiasvivas.engine.agency.ActionExecutionStatus
import com.historiasvivas.engine.agency.AutonomousActionPlan
import com.historiasvivas.engine.agency.AutonomousActionResult
import com.historiasvivas.engine.agency.AutonomousActionKind
import com.historiasvivas.engine.model.Story

class AutonomousActionSignalGenerator {
 fun generate(story:Story,plan:AutonomousActionPlan,result:AutonomousActionResult):WorldSignal?{
  if(result.status!=ActionExecutionStatus.STARTED || result.event==null)return null
  return when(plan.kind){
   AutonomousActionKind.TRAVEL->{
    val journey=story.world.spatial.journeys[plan.characterId]?:return null
    WorldSignal(
     id="trace-"+journey.characterId+"-"+journey.startedTurn,
     locationId=null,routeFrom=journey.from,routeTo=journey.to,
     sense=SignalSense.TRACE,
     surface="Fresh signs show that someone has just entered the route",
     hiddenCause="autonomous-travel:"+plan.characterId+":"+plan.id,
     createdTurn=story.turn,salience=55,persistent=true
    )
   }
   else->null
  }
 }
}
