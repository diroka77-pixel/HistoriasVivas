package com.historiasvivas.engine.turn
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.context.ContextSelector
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.validation.ContinuityValidator
import com.historiasvivas.engine.offscreen.OffscreenSimulator
import com.historiasvivas.engine.director.*

class TurnEngine(
 private val ai:NarrativeAI,
 private val selector:ContextSelector=ContextSelector(),
 private val reducer:WorldReducer=WorldReducer(),
 private val validator:ContinuityValidator=ContinuityValidator(),
 private val offscreen:OffscreenSimulator=OffscreenSimulator(),
 private val causalWorld:CausalWorldEngine=CausalWorldEngine(),
 private val pacing:EmergentPacingDirector=EmergentPacingDirector()
){
 suspend fun play(story:Story,userAction:String):TurnResult{
  require(story.status==StoryStatus.ACTIVE){"Story is not active"}
  require(userAction.isNotBlank()){"User action cannot be blank"}
  val context=selector.select(story,userAction)
  val response=ai.continueStory(NarrativeRequest(story,userAction,context))
  val event=StoryEvent(
   id="turn-"+(story.turn+1),turn=story.turn+1,
   summary=response.eventSummary.ifBlank{userAction.take(240)},
   participants=response.participantIds,tags=response.tags,permanent=response.canonical
  )
  val present=(response.participantIds+setOf(story.protagonistId)).filter{it in story.world.characters}.toSet()
  val offscreenHidden=offscreen.plan(story,present,maxActions=2).map{it.hiddenEvent}
  val causal=causalWorld.advance(story,story.director.lastEmergentTurn)
  val pacingDecision=pacing.decide(story,causal,context.narrativeBeat.mode)
  val surfaced=if(pacingDecision.focus==EmergentFocus.SURFACE_NOW)causal.publicEvents else emptyList()
  val latentPublic=if(pacingDecision.focus==EmergentFocus.SURFACE_NOW) emptyMap() else causal.publicEvents.associateBy{it.id}
  val latentIds=if(pacingDecision.focus==EmergentFocus.SURFACE_NOW) emptySet() else (causal.publicEvents.map{it.id}+causal.hiddenEvents.map{it.id}).toSet()
  val hidden=offscreenHidden+causal.hiddenEvents
  val delta=WorldDelta(
   event,response.proposedFacts,response.proposedConsequences+causal.consequences,response.resolvedConsequenceIds,
   response.proposedMindDeltas,response.proposedPerceptionDeltas,hidden,surfaced
  )
  val validation=validator.validate(story,delta)
  require(validation.accepted){"AI proposed an inconsistent world delta: "+validation.issues.joinToString{it.code}}
  val reduced=reducer.apply(story,delta)
  val updatedDirector=reduced.director.copy(
   lastEmergentTurn=if(pacingDecision.focus==EmergentFocus.SURFACE_NOW) causal.emergentTurn?:reduced.director.lastEmergentTurn else reduced.director.lastEmergentTurn,
   latentEmergentEventIds=reduced.director.latentEmergentEventIds+latentIds,
   latentEmergentEvents=reduced.director.latentEmergentEvents+latentPublic
  )
  return TurnResult(reduced.copy(director=updatedDirector),response.prose,delta)
 }
}
