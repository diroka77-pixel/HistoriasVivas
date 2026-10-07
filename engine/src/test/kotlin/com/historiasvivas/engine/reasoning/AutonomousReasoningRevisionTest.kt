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
}
