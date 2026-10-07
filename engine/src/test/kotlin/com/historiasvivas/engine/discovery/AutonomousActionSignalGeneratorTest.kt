package com.historiasvivas.engine.discovery

import com.historiasvivas.engine.agency.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*

class AutonomousActionSignalGeneratorTest {
 @Test fun startedTravelProducesAnonymousObservableTrace(){
  val journey=Journey("erik","settlement","ruins",3,0,7)
  val story=Story("s","Nadir","p",turn=7,world=WorldState(spatial=SpatialState(journeys=mapOf("erik" to journey))))
  val plan=AutonomousActionPlan("plan","erik","h",AutonomousActionKind.TRAVEL,70,"ruins",motivation="private",turn=7)
  val event=StoryEvent("departure",7,"Erik leaves",participants=setOf("erik"))
  val signal=AutonomousActionSignalGenerator().generate(story,plan,AutonomousActionResult("plan",ActionExecutionStatus.STARTED,"Journey started",event))
  assertNotNull(signal)
  assertEquals("trace-erik-7",signal.id)
  assertFalse(signal.surface.contains("erik",ignoreCase=true))
  assertTrue(signal.hiddenCause!!.contains("erik"))
 }
 @Test fun deferredPrivatePlanDoesNotBecomePhysicalEvidence(){
  val story=Story("s","Nadir","p",turn=7)
  val plan=AutonomousActionPlan("plan","erik","h",AutonomousActionKind.HIDE_EVIDENCE,80,motivation="private",turn=7)
  val result=AutonomousActionResult("plan",ActionExecutionStatus.DEFERRED,"Requires resolution")
  assertNull(AutonomousActionSignalGenerator().generate(story,plan,result))
 }
}
