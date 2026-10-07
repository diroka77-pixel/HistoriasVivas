package com.historiasvivas.engine.discovery
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.spatial.*
import com.historiasvivas.engine.world.*
import kotlin.test.*
class CausalDiscoveryGeneratorTest {
 @Test fun noCauseMeansNoManufacturedDiscovery(){
  val story=Story("s","Nadir","p")
  assertTrue(CausalDiscoveryGenerator().generate(story).isEmpty())
 }
 @Test fun realJourneyLeavesARealTrace(){
  val spatial=SpatialState(
   positions=mapOf("erik" to CharacterPosition("erik","ruins")),
   routes=listOf(Route("ruins","settlement",3)),
   journeys=mapOf("erik" to Journey("erik","ruins","settlement",3,1,8))
  )
  val story=Story("s","Nadir","p",world=WorldState(spatial=spatial),turn=9)
  val signal=CausalDiscoveryGenerator().generate(story).single()
  assertEquals(SignalSense.TRACE,signal.sense)
  assertEquals("ruins",signal.routeFrom)
  assertTrue(signal.hiddenCause!!.contains("erik"))
  assertFalse(signal.surface.contains("erik",ignoreCase=true))
 }
 @Test fun risingFaunaPressureCreatesObservableEvidence(){
  val env=EnvironmentState(pressures=mapOf("fauna" to WorldPressure("fauna",70,1,"Native fauna is probing the perimeter")))
  val story=Story("s","Nadir","p",world=WorldState(environment=env),turn=12)
  val signal=CausalDiscoveryGenerator().generate(story).single()
  assertEquals(SignalSense.ENVIRONMENT,signal.sense)
  assertTrue(signal.surface.contains("tracks",ignoreCase=true))
 }
}