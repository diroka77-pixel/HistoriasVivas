package com.historiasvivas.engine.turn
import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.reasoning.Hypothesis
import com.historiasvivas.engine.social.DisclosureDecision

class CognitiveWorldReducer {
 fun rememberHypothesis(story:Story,hypothesis:Hypothesis):Story{
  val reasoning=story.world.reasoning.copy(
   hypotheses=story.world.reasoning.hypotheses+(hypothesis.id to hypothesis)
  )
  return story.copy(world=story.world.copy(reasoning=reasoning))
 }

 fun rememberDisclosure(story:Story,decision:DisclosureDecision):Story{
  val social=story.world.socialKnowledge.copy(
   disclosureHistory=story.world.socialKnowledge.disclosureHistory+(decision.id to decision)
  )
  return story.copy(world=story.world.copy(socialKnowledge=social))
 }

 fun rememberPlan(story:Story,plan:AutonomousActionPlan):Story{
  val agency=story.world.autonomousAgency.copy(
   plans=story.world.autonomousAgency.plans+(plan.id to plan)
  )
  return story.copy(world=story.world.copy(autonomousAgency=agency))
 }

 fun rememberResult(story:Story,result:AutonomousActionResult):Story{
  val agency=story.world.autonomousAgency.copy(
   results=story.world.autonomousAgency.results+(result.planId to result)
  )
  return story.copy(world=story.world.copy(autonomousAgency=agency))
 }
}
