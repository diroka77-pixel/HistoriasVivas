package com.historiasvivas.engine.agency
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.reasoning.AutonomousReasoningEngine
import com.historiasvivas.engine.spatial.SpatialEngine
import com.historiasvivas.engine.offscreen.HiddenEvent

data class AutonomousCycle(
 val plans:List<AutonomousActionPlan> = emptyList(),
 val results:List<AutonomousActionResult> = emptyList()
)

class AutonomousTurnScheduler(
 private val reasoning:AutonomousReasoningEngine=AutonomousReasoningEngine(),
 private val planner:HypothesisAgencyEngine=HypothesisAgencyEngine(),
 private val executor:AutonomousActionExecutor=AutonomousActionExecutor(),
 private val spatial:SpatialEngine=SpatialEngine(),
 private val cooldownTurns:Long=3
){
 fun run(story:Story,presentCharacterIds:Set<String>,maxCharacters:Int=1):Pair<Story,AutonomousCycle>{
  if(maxCharacters<=0)return story to AutonomousCycle()
  val recentlyPlanned=story.world.autonomousAgency.plans.values
   .filter{story.turn-it.turn<cooldownTurns}.map{it.characterId}.toSet()
  val eligible=story.world.characters.values.asSequence()
   .filter{it.alive&&it.id !in presentCharacterIds&&it.id !in recentlyPlanned}
   .filter{spatial.locationOf(story.world.spatial,it.id)!=null}
   .sortedBy{it.id}.take(maxCharacters).toList()

  var current=story
  val plans=mutableListOf<AutonomousActionPlan>()
  val results=mutableListOf<AutonomousActionResult>()
  eligible.forEach{character->
   val inference=reasoning.infer(current,character.id,current.turn).firstOrNull()?:return@forEach
   val plan=planner.plan(current,character.id,inference.hypothesis)?:return@forEach
   val (after,result)=executor.execute(current,plan)
   val actionHistory=result.event?.let{event-> HiddenEvent(id="hidden-"+event.id,turn=event.turn,actorId=plan.characterId,summary=event.summary) }
   val agency=after.world.autonomousAgency.copy(
    plans=after.world.autonomousAgency.plans+(plan.id to plan),
    results=after.world.autonomousAgency.results+(plan.id to result)
   )
   val reasoningState=after.world.reasoning.copy(
    hypotheses=after.world.reasoning.hypotheses+(inference.hypothesis.id to inference.hypothesis)
   )
   current=after.copy(world=after.world.copy(autonomousAgency=agency,reasoning=reasoningState,hiddenEvents=(after.world.hiddenEvents+listOfNotNull(actionHistory)).distinctBy{it.id}))
   plans+=plan; results+=result
  }
  return current to AutonomousCycle(plans,results)
 }
}
