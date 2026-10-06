package com.historiasvivas.engine.turn
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.context.ContextSelector
import com.historiasvivas.engine.model.*
class TurnEngine(private val ai:NarrativeAI,private val selector:ContextSelector=ContextSelector(),private val reducer:WorldReducer=WorldReducer()){
 suspend fun play(story:Story,userAction:String):TurnResult{
  require(story.status==StoryStatus.ACTIVE){"Story is not active"}
  require(userAction.isNotBlank()){"User action cannot be blank"}
  val context=selector.select(story,userAction)
  val response=ai.continueStory(NarrativeRequest(story,userAction,context))
  val event=StoryEvent(id="turn-"+(story.turn+1),turn=story.turn+1,summary=response.eventSummary.ifBlank{userAction.take(240)},participants=response.participantIds,tags=response.tags,permanent=response.canonical)
  val delta=WorldDelta(event,response.proposedFacts,response.proposedConsequences,response.resolvedConsequenceIds)
  return TurnResult(reducer.apply(story,delta),response.prose,delta)
 }
}