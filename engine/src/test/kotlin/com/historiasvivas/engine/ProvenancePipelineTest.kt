package com.historiasvivas.engine

import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.social.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*

class ProvenancePipelineTest {
 @Test fun privateActionProvenanceSurvivesEvidenceTestimonyAndRumor(){
  val hidden=HiddenEvent("hidden-departure",7,"erik","Erik leaves for the ruins")
  val journey=Journey("erik","settlement","ruins",3,0,7)
  val story=Story("s","Nadir","p",turn=7,world=WorldState(spatial=SpatialState(journeys=mapOf("erik" to journey))))
  val plan=AutonomousActionPlan("plan","erik","h",AutonomousActionKind.TRAVEL,70,"ruins",motivation="private",turn=7)
  val result=AutonomousActionResult(
   "plan",ActionExecutionStatus.STARTED,"Journey started",
   StoryEvent("departure",7,"Erik leaves",participants=setOf("erik"))
  )
  val signal=AutonomousActionSignalGenerator().generate(story,plan,result)!!
  assertEquals(hidden.id,signal.relatedEventId)

  val investigator=InvestigationEngine()
  val first=investigator.investigate(
   DiscoveryState(signals=mapOf(signal.id to signal)),signal.id,"elena",CharacterMind(),8
  )!!
  val reopened=first.state.copy(signals=first.state.signals+(signal.id to first.state.signals.getValue(signal.id).copy(investigatedBy=emptySet())))
  val second=investigator.investigate(reopened,signal.id,"elena",CharacterMind(),9)!!
  assertTrue(second.state.evidence.values.all{it.relatedEventId==hidden.id})

  val hypothesis=HypothesisEngine().formulate(
   "elena","ruins","Someone left for the ruins",
   second.state.evidence.values.toList(),emptyList(),10
  )!!
  assertEquals(hidden.id,hypothesis.relatedEventId)

  val elena=Character("elena","Elena",mind=CharacterMind(personality=PersonalityProfile(mapOf("honesty" to 70,"loyalty" to 60))))
  val daniel=Character("daniel","Daniel")
  val disclosureStory=Story(
   "s","Nadir","p",turn=10,
   world=WorldState(
    characters=mapOf("elena" to elena,"daniel" to daniel),
    relationships=listOf(Relationship("elena","daniel",trust=70))
   )
  )
  val disclosure=DisclosureEngine()
  val decision=disclosure.decide(disclosureStory,"elena","daniel",hypothesis)!!
  val testimony=disclosure.toTestimony(decision,hypothesis)!!
  assertEquals(hidden.id,testimony.relatedEventId)

  val social=SocialInformationEngine()
  val heard=social.hear(testimony,"daniel",daniel.mind)
  assertEquals(hidden.id,heard.relatedEventId)
  val directlyDiscovered=KnowledgeGate().discover(
   listOf(hidden),
   Discovery(hidden.id,"daniel",DiscoveryChannel.TOLD,"Elena tells Daniel what she found",sourceId=testimony.id),
   DiscoverySources(
    testimonies=mapOf(testimony.id to testimony),
    heardClaims=mapOf(testimony.id+":"+heard.listenerId to heard)
   )
  )
  assertEquals(listOf(hidden.id),KnowledgeGate().visible(directlyDiscovered,"daniel").map{it.id})

  val rumor=RumorEngine().relay(heard,"daniel","maya",11)
  val heardByMaya=social.hear(rumor,"maya",CharacterMind())
  val rumorBelief=social.asBelief(heardByMaya)
  assertEquals(hidden.id,rumor.relatedEventId)
  assertEquals(hidden.id,heardByMaya.relatedEventId)
  val rumorAttempt=KnowledgeGate().discover(
   listOf(hidden),
   Discovery(hidden.id,"maya",DiscoveryChannel.RUMOR,"Daniel relays Elena's account",sourceId=rumor.id),
   DiscoverySources(
    testimonies=mapOf(rumor.id to rumor),
    heardClaims=mapOf(rumor.id+":"+heardByMaya.listenerId to heardByMaya)
   )
  )
  assertEquals(rumor.claim,rumorBelief.claim)
  assertTrue(KnowledgeGate().visible(rumorAttempt,"maya").isEmpty())
 }
}
