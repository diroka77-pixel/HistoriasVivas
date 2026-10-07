package com.historiasvivas.engine.agency

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*

class AutonomousCognitionEndToEndTest {
 @Test fun npcCanInvestigateInferAndPlanWithoutLeakingKnowledgeToProtagonist(){
  val erik=Character("erik","Erik",mind=CharacterMind(personality=PersonalityProfile(mapOf("courage" to 60))))
  val daniel=Character("daniel","Daniel")
  val signals=listOf(
   WorldSignal("pulse-a","ruins",sense=SignalSense.RADIO,surface="A repeating pulse comes from below",hiddenCause="buried beacon",createdTurn=8,salience=80),
   WorldSignal("pulse-b","ruins",sense=SignalSense.RADIO,surface="A second synchronized pulse answers nearby",hiddenCause="relay node",createdTurn=9,salience=80)
  )
  var story=Story(
   "s","Nadir","p",
   world=WorldState(
    characters=mapOf("erik" to erik,"daniel" to daniel),
    locations=mapOf("ruins" to Location("ruins","Ruins")),
    spatial=SpatialState(positions=mapOf("erik" to CharacterPosition("erik","ruins"),"daniel" to CharacterPosition("daniel","settlement"))),
    discovery=DiscoveryState(signals=signals.associateBy{it.id})
   ),
   turn=12,protagonistId="daniel"
  )

  val investigator=InvestigationEngine()
  for(signal in signals){
   val outcome=investigator.investigate(story.world.discovery,signal.id,"erik",erik.mind,story.turn)!!
   story=story.copy(world=story.world.copy(discovery=outcome.state))
  }

  assertEquals(2,story.world.discovery.evidence.values.count{it.discoveredBy=="erik"})
  assertTrue(story.world.discovery.evidence.values.none{it.discoveredBy=="daniel"})
  signals.forEach{
   assertEquals(SignalStatus.INVESTIGATED,story.world.discovery.signals.getValue(it.id).statusFor("erik"))
   assertEquals(SignalStatus.UNNOTICED,story.world.discovery.signals.getValue(it.id).statusFor("daniel"))
  }

  val inference=AutonomousReasoningEngine().infer(story,"erik").single()
  assertEquals("erik",inference.hypothesis.characterId)
  assertEquals(2,inference.hypothesis.supportingEvidenceIds.size)
  val plan=HypothesisAgencyEngine().plan(story,"erik",inference.hypothesis)!!
  val (acted,result)=AutonomousActionExecutor().execute(story,plan)
  assertEquals("erik",plan.characterId)
  assertEquals(inference.hypothesis.id,plan.hypothesisId)
  assertTrue(plan.motivation.contains(inference.hypothesis.claim))
  assertTrue(result.status in setOf(ActionExecutionStatus.STARTED,ActionExecutionStatus.BLOCKED,ActionExecutionStatus.DEFERRED))
  if(result.status==ActionExecutionStatus.STARTED&&plan.kind==AutonomousActionKind.INVESTIGATE){
   assertTrue(acted.world.discovery.evidence.values.count{it.discoveredBy=="erik"}>=2)
  }
  assertTrue(acted.world.discovery.evidence.values.none{it.discoveredBy=="daniel"})
  assertTrue(story.world.reasoning.hypotheses.values.none{it.characterId=="daniel"})
  assertTrue(story.world.discovery.interpretations.values.none{it.characterId=="daniel"})
 }
}
