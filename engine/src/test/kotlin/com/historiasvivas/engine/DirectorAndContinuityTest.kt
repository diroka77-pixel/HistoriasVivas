package com.historiasvivas.engine
import com.historiasvivas.engine.director.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.turn.*
import com.historiasvivas.engine.validation.*
import kotlin.test.*

class DirectorAndContinuityTest {
 @Test fun directorSurfacesOldDueConsequence(){
  val consequence=Consequence("return","old-event","An old enemy returns",20)
  val story=Story("s","Story","Premise",turn=20,world=WorldState(pendingConsequences=listOf(consequence)))
  val plan=DirectorEngine().plan(story,DirectorState(tension=40,quietTurns=2))
  assertEquals("return",plan.consequenceToSurface)
  assertTrue(plan.targetTension>40)
 }
 @Test fun validatorRejectsUnknownCharacter(){
  val story=Story("s","Story","Premise")
  val delta=WorldDelta(StoryEvent("e",1,"Someone appears",participants=setOf("ghost")))
  val result=ContinuityValidator().validate(story,delta)
  assertFalse(result.accepted)
  assertTrue(result.issues.any{it.code=="UNKNOWN_CHARACTER"})
 }
 @Test fun directorAllowsBreathingAfterHighTension(){
  val story=Story("s","Story","Premise",turn=12)
  val plan=DirectorEngine().plan(story,DirectorState(tension=90))
  assertTrue(plan.allowQuietMoment)
  assertTrue(plan.targetTension<90)
 }
}
