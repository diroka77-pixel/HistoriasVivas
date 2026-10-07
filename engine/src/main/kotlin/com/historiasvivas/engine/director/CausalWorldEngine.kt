package com.historiasvivas.engine.director
import com.historiasvivas.engine.model.*

data class CausalWorldResult(
 val publicEvents:List<StoryEvent> = emptyList(),
 val hiddenEvents:List<com.historiasvivas.engine.offscreen.HiddenEvent> = emptyList(),
 val consequences:List<Consequence> = emptyList(),
 val emergentTurn:Long?=null
)

class CausalWorldEngine(
 private val director:CausalEventDirector=CausalEventDirector(),
 private val materializer:CausalEventMaterializer=CausalEventMaterializer(),
 private val validator:CausalEventValidator=CausalEventValidator()
){
 fun advance(story:Story,lastEmergentTurn:Long?=null):CausalWorldResult{
  val proposal=director.propose(story,lastEmergentTurn)?:return CausalWorldResult()
  val candidate=materializer.materialize(story,proposal)?:return CausalWorldResult()
  if(!validator.validate(story,candidate).accepted)return CausalWorldResult()
  return CausalWorldResult(
   publicEvents=listOfNotNull(candidate.event),
   hiddenEvents=listOfNotNull(candidate.hiddenEvent),
   consequences=candidate.consequences,
   emergentTurn=story.turn+1
  )
 }
}