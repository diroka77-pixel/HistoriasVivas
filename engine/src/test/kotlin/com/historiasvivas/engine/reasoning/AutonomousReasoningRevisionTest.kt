package com.historiasvivas.engine.reasoning

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.model.*
import kotlin.test.*

class AutonomousReasoningRevisionTest {
 @Test fun newCompatibleEvidenceReinforcesExistingHypothesisInsteadOfDuplicatingIt(){
  val claim="The ruins may contain an active source"
  val e1=Evidence("e1","pulse-a","First pulse",70,8,"erik")
  val e2=Evidence("e2","pulse-b","Second pulse",70,9,"erik")
  val e3=Evidence("e3","pulse-c","Third pulse",80,12,"erik")
  val interpretation=Interpretation("i","erik","pulse-a",claim,70,setOf("e1","e2","e3"))
  val old=Hypothesis("old","erik","pulse-a+pulse-b",claim,55,setOf("e1","e2"),createdTurn=9,revisedTurn=9,status=HypothesisStatus.PLAUSIBLE)
  val story=Story(
   "s","Nadir","p",
   world=WorldState(
    characters=mapOf("erik" to Character("erik","Erik",mind=CharacterMind())),
    discovery=DiscoveryState(
     evidence=mapOf(e1.id to e1,e2.id to e2,e3.id to e3),
     interpretations=mapOf(interpretation.id to interpretation)
    ),
    reasoning=ReasoningState(hypotheses=mapOf(old.id to old))
   ),
   turn=12
  )
  val revised=AutonomousReasoningEngine().infer(story,"erik").single().hypothesis
  assertEquals("old",revised.id)
  assertEquals(setOf("e1","e2","e3"),revised.supportingEvidenceIds)
  assertTrue(revised.confidence>old.confidence)
  assertEquals(12,revised.revisedTurn)
 }
 @Test fun explicitContradictoryEvidenceWeakensExistingHypothesisWithoutTextGuessing(){
  val claim="The ruins may contain an active source"
  val e1=Evidence("e1","pulse-a","First pulse",70,8,"erik")
  val e2=Evidence("e2","pulse-b","Second pulse",70,9,"erik")
  val e3=Evidence("e3","silence","A complete scan finds no active emission",90,12,"erik")
  val support=Interpretation("support","erik","pulse-a",claim,70,setOf("e1","e2"))
  val contradiction=Interpretation("against","erik","silence","The scan conflicts with the active-source theory",80,setOf("e3"),relation=InterpretationRelation.CONTRADICTS,relatedHypothesisId="old")
  val old=Hypothesis("old","erik","pulse-a+pulse-b",claim,55,setOf("e1","e2"),createdTurn=9,revisedTurn=9,status=HypothesisStatus.PLAUSIBLE)
  val story=Story(
   "s","Nadir","p",
   world=WorldState(
    characters=mapOf("erik" to Character("erik","Erik",mind=CharacterMind())),
    discovery=DiscoveryState(evidence=mapOf(e1.id to e1,e2.id to e2,e3.id to e3),interpretations=mapOf(support.id to support,contradiction.id to contradiction)),
    reasoning=ReasoningState(hypotheses=mapOf(old.id to old))
   ),turn=12
  )
  val revised=AutonomousReasoningEngine().infer(story,"erik").single().hypothesis
  assertEquals("old",revised.id)
  assertTrue(revised.confidence<old.confidence)
  assertEquals(setOf("e3"),revised.contradictingEvidenceIds)
  assertEquals(HypothesisStatus.WEAKENED,revised.status)
 }

 @Test fun sameClaimAtSameLocationCombinesIntoLocationGroundedHypothesis(){
  val e1=Evidence("e1","a","Pulse A",70,1,"erik"); val e2=Evidence("e2","b","Pulse B",70,2,"erik")
  val claim="The signal may have an intentional source"
  val state=DiscoveryState(
   signals=mapOf(
    "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Pulse A",createdTurn=1),
    "b" to WorldSignal("b","ruins",sense=SignalSense.RADIO,surface="Pulse B",createdTurn=2)
   ),
   evidence=mapOf("e1" to e1,"e2" to e2),
   interpretations=mapOf(
    "i1" to Interpretation("i1","erik","a",claim,60,setOf("e1")),
    "i2" to Interpretation("i2","erik","b",claim,60,setOf("e2"))
   )
  )
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to Character("erik","Erik")),discovery=state),turn=3)
  assertEquals("ruins",AutonomousReasoningEngine().infer(story,"erik").single().hypothesis.subject)
 }
 @Test fun sameGenericClaimAtDifferentLocationsDoesNotCreateFalseCombinedHypothesis(){
  val e1=Evidence("e1","a","Pulse A",70,1,"erik"); val e2=Evidence("e2","b","Pulse B",70,2,"erik")
  val claim="The signal may have an intentional source"
  val state=DiscoveryState(
   signals=mapOf(
    "a" to WorldSignal("a","ruins",sense=SignalSense.RADIO,surface="Pulse A",createdTurn=1),
    "b" to WorldSignal("b","forest",sense=SignalSense.RADIO,surface="Pulse B",createdTurn=2)
   ),
   evidence=mapOf("e1" to e1,"e2" to e2),
   interpretations=mapOf(
    "i1" to Interpretation("i1","erik","a",claim,60,setOf("e1")),
    "i2" to Interpretation("i2","erik","b",claim,60,setOf("e2"))
   )
  )
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to Character("erik","Erik")),discovery=state),turn=3)
  assertTrue(AutonomousReasoningEngine().infer(story,"erik").isEmpty())
 }

}
