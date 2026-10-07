package com.historiasvivas.engine.agency
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.spatial.SpatialEngine

@Serializable enum class ActionExecutionStatus { STARTED, BLOCKED, DEFERRED }
@Serializable data class AutonomousActionResult(
 val planId:String,val status:ActionExecutionStatus,val reason:String,val event:StoryEvent?=null
)

class AutonomousActionExecutor(private val spatial:SpatialEngine=SpatialEngine()){
 fun execute(story:Story,plan:AutonomousActionPlan):Pair<Story,AutonomousActionResult>{
  val c=story.world.characters[plan.characterId]
  if(c==null||!c.alive)return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Character unavailable")
  return when(plan.kind){
   AutonomousActionKind.TRAVEL->{
    val to=plan.targetLocationId?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No destination")
    val before=story.world.spatial; val after=spatial.startJourney(before,plan.characterId,to,story.turn)
    if(after==before) story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No viable route")
    else {
     val event=StoryEvent(plan.id+"-departure",story.turn,c.name+" leaves to pursue a private lead",setOf(c.id),setOf("autonomous","travel"))
     story.copy(world=story.world.copy(spatial=after)) to AutonomousActionResult(plan.id,ActionExecutionStatus.STARTED,"Journey started",event)
    }
   }
   AutonomousActionKind.INVESTIGATE,AutonomousActionKind.WARN,AutonomousActionKind.PROTECT,AutonomousActionKind.HIDE_EVIDENCE,AutonomousActionKind.WITHDRAW->
    story to AutonomousActionResult(plan.id,ActionExecutionStatus.DEFERRED,"Requires scene/world-specific resolution")
  }
 }
}