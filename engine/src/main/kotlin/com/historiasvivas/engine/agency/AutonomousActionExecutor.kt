package com.historiasvivas.engine.agency
import kotlinx.serialization.Serializable
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.spatial.SpatialEngine
import com.historiasvivas.engine.discovery.DiscoveryEngine
import com.historiasvivas.engine.discovery.InvestigationEngine

@Serializable enum class ActionExecutionStatus { STARTED, BLOCKED, DEFERRED }
@Serializable data class AutonomousActionResult(
 val planId:String,val status:ActionExecutionStatus,val reason:String,val event:StoryEvent?=null
)

class AutonomousActionExecutor(
 private val spatial:SpatialEngine=SpatialEngine(),
 private val discovery:DiscoveryEngine=DiscoveryEngine(),
 private val investigation:InvestigationEngine=InvestigationEngine()
){
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
   AutonomousActionKind.INVESTIGATE->{
    val location=spatial.locationOf(story.world.spatial,plan.characterId)
    val signal=discovery.visibleAt(story.world.discovery,plan.characterId,location).firstOrNull()
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No observable signal to investigate")
    val outcome=investigation.investigate(story.world.discovery,signal.id,plan.characterId,c.mind,story.turn)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Investigation could not resolve")
    val event=StoryEvent(
     plan.id+"-investigation",story.turn,c.name+" investigates an observable anomaly",
     setOf(c.id),setOf("autonomous","investigation")
    )
    story.copy(world=story.world.copy(discovery=outcome.state)) to
     AutonomousActionResult(plan.id,ActionExecutionStatus.STARTED,"Observable signal investigated",event)
   }
   AutonomousActionKind.WITHDRAW->{
    val from=spatial.locationOf(story.world.spatial,plan.characterId)
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"Character is not at a stable location")
    val destination=story.world.spatial.routes.asSequence()
     .filter{!it.blocked}
     .mapNotNull{route->
      when{
       route.from==from->route.to
       route.bidirectional&&route.to==from->route.from
       else->null
      }
     }
     .filter{it!=plan.targetLocationId}
     .distinct().sorted().firstOrNull()
     ?:return story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No safe withdrawal route")
    val after=spatial.startJourney(story.world.spatial,plan.characterId,destination,story.turn)
    if(after==story.world.spatial) story to AutonomousActionResult(plan.id,ActionExecutionStatus.BLOCKED,"No viable withdrawal route")
    else{
     val event=StoryEvent(plan.id+"-withdrawal",story.turn,c.name+" withdraws from a perceived threat",setOf(c.id),setOf("autonomous","withdraw"))
     story.copy(world=story.world.copy(spatial=after)) to AutonomousActionResult(plan.id,ActionExecutionStatus.STARTED,"Withdrawal started",event)
    }
   }
   AutonomousActionKind.WARN,AutonomousActionKind.PROTECT,AutonomousActionKind.HIDE_EVIDENCE->
    story to AutonomousActionResult(plan.id,ActionExecutionStatus.DEFERRED,"Requires scene/world-specific resolution")
  }
 }
}