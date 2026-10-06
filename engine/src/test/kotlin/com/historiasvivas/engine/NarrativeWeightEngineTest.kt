package com.historiasvivas.engine
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.narrative.*
import kotlin.test.*

class NarrativeWeightEngineTest {
 @Test fun ordinaryMomentIsAllowedToBreathe(){
  val beat=NarrativeWeightEngine().assess(Story("s","Story","Premise"),"Miro la lluvia por la ventana.")
  assertEquals(ResponseMode.BREATHE,beat.mode)
  assertFalse(beat.shouldOfferChoices)
  assertTrue(beat.preservePlayerDecisionSpace)
 }
 @Test fun meaningfulActionGetsSpaceWithoutStealingNextDecision(){
  val beat=NarrativeWeightEngine().assess(Story("s","Story","Premise"),"Pregunto a Elena qué ocurrió anoche.")
  assertEquals(ResponseMode.DEVELOP,beat.mode)
  assertFalse(beat.shouldOfferChoices)
  assertTrue(beat.preservePlayerDecisionSpace)
 }
 @Test fun oldConsequenceCanSurfaceWithoutForcedEscalation(){
  val consequence=Consequence("c","e","Erik remembers being abandoned",1)
  val story=Story("s","Story","Premise",world=WorldState(pendingConsequences=listOf(consequence)))
  val beat=NarrativeWeightEngine().assess(story,"Camino por el asentamiento.")
  assertEquals(ResponseMode.REACT_SUBTLY,beat.mode)
 }
}
