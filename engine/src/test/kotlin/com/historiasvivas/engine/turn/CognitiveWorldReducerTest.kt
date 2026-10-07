package com.historiasvivas.engine.turn
import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.reasoning.*
import com.historiasvivas.engine.social.*
import kotlin.test.*

class CognitiveWorldReducerTest {
 @Test fun cognitiveHistoryPersistsWithoutBecomingPublicFact(){
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("erik" to Character("erik","Erik"))))
  val hypothesis=Hypothesis("h","erik","ruins","A hidden transmitter may be active",80,createdTurn=3,status=HypothesisStatus.STRONG)
  val decision=DisclosureDecision("d","erik","daniel","h",DisclosureKind.DELAY,4,"private motivation")
  val plan=AutonomousActionPlan("p","erik","h",AutonomousActionKind.INVESTIGATE,80,motivation="private hypothesis",turn=4)
  val reducer=CognitiveWorldReducer()
  val updated=reducer.rememberPlan(reducer.rememberDisclosure(reducer.rememberHypothesis(story,hypothesis),decision),plan)
  assertEquals(hypothesis,updated.world.reasoning.hypotheses["h"])
  assertEquals(decision,updated.world.socialKnowledge.disclosureHistory["d"])
  assertEquals(plan,updated.world.autonomousAgency.plans["p"])
  assertFalse(updated.world.facts.any{it.contains("transmitter",true)})
 }
}
