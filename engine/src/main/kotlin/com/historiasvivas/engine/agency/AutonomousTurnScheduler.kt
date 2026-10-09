package com.historiasvivas.engine.agency
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.reasoning.AutonomousReasoningEngine
import com.historiasvivas.engine.spatial.SpatialEngine
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.discovery.AutonomousActionSignalGenerator

data class AutonomousCycle(
 val plans:List<AutonomousActionPlan> = emptyList(),
 val results:List<AutonomousActionResult> = emptyList()
)

class AutonomousActionHistory {
 fun materialize(
  plan:AutonomousActionPlan,
  result:AutonomousActionResult
 ):HiddenEvent? = result.event?.let{event->
  HiddenEvent(
   id="hidden-"+event.id,
   turn=event.turn,
   actorId=plan.characterId,
   summary=event.summary,
   knownBy=result.eventWitnessIds+plan.characterId
  )
 }
}

class AutonomousTurnScheduler(
 private val reasoning:AutonomousReasoningEngine=AutonomousReasoningEngine(),
 private val planner:HypothesisAgencyEngine=HypothesisAgencyEngine(),
 private val executor:AutonomousActionExecutor=AutonomousActionExecutor(),
 private val spatial:SpatialEngine=SpatialEngine(),
 private val signalGenerator:AutonomousActionSignalGenerator=AutonomousActionSignalGenerator(),
 private val history:AutonomousActionHistory=AutonomousActionHistory(),
 private val cooldownTurns:Long=3
){
 fun run(story:Story,presentCharacterIds:Set<String>,maxCharacters:Int=1):Pair<Story,AutonomousCycle>{
  if(maxCharacters<=0)return story to AutonomousCycle()
  val recentlyPlanned=story.world.autonomousAgency.plans.values
   .filter{plan->
    val result=story.world.autonomousAgency.results[plan.id]
    result?.status!=ActionExecutionStatus.BLOCKED&&story.turn-plan.turn<cooldownTurns
   }.map{it.characterId}.toSet()
  val alreadyActedThisTurn=story.world.hiddenEvents.asSequence()
   .filter{it.turn==story.turn}.map{it.actorId}.toSet()
  val eligible=story.world.characters.values.asSequence()
   .filter{it.alive&&it.id !in presentCharacterIds&&it.id !in recentlyPlanned&&it.id !in alreadyActedThisTurn}
   .filter{spatial.locationOf(story.world.spatial,it.id)!=null}
   .sortedBy{it.id}.toList()

  var current=story
  val plans=mutableListOf<AutonomousActionPlan>()
  val results=mutableListOf<AutonomousActionResult>()
  eligible.forEach{character->
   if(plans.size>=maxCharacters)return@forEach
   val inference=reasoning.infer(current,character.id,current.turn).firstOrNull()?:return@forEach
   current=current.copy(world=current.world.copy(reasoning=current.world.reasoning.copy(
    hypotheses=current.world.reasoning.hypotheses+(inference.hypothesis.id to inference.hypothesis)
   )))
   val plan=planner.plan(current,character.id,inference.hypothesis)?:return@forEach
   val (after,result)=executor.execute(current,plan)
   val actionHistory=history.materialize(plan,result)
   val generatedSignal=signalGenerator.generate(after,plan,result)
   val discovery=generatedSignal?.let{signal->
    after.world.discovery.copy(signals=after.world.discovery.signals+(signal.id to signal))
   }?:after.world.discovery
   val agency=after.world.autonomousAgency.copy(
    plans=after.world.autonomousAgency.plans+(plan.id to plan),
    results=after.world.autonomousAgency.results+(plan.id to result)
   )
   current=after.copy(world=after.world.copy(
    autonomousAgency=agency,
    hiddenEvents=(after.world.hiddenEvents+listOfNotNull(actionHistory)).distinctBy{it.id},
    discovery=discovery
   ))
   plans+=plan; results+=result
  }
  return current to AutonomousCycle(plans,results)
 }
}
