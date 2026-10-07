package com.historiasvivas.engine.spatial
import kotlin.test.*
class JourneyTest {
 @Test fun travellerIsNowhereUntilArrival(){
  val engine=SpatialEngine()
  var state=SpatialState(
   positions=mapOf("daniel" to CharacterPosition("daniel","settlement")),
   routes=listOf(Route("settlement","ruins",3))
  )
  state=engine.startJourney(state,"daniel","ruins",5)
  assertNull(engine.locationOf(state,"daniel"))
  assertFalse("daniel" in engine.presentAt(state,"settlement"))
  assertFalse("daniel" in engine.presentAt(state,"ruins"))
  state=engine.advanceJourneys(state,1,6)
  assertEquals(2,state.journeys["daniel"]?.remainingUnits)
  state=engine.advanceJourneys(state,2,8)
  assertEquals("ruins",engine.locationOf(state,"daniel"))
  assertTrue("daniel" in engine.presentAt(state,"ruins"))
 }
 @Test fun journeySurvivesSerializationStateShape(){
  val state=SpatialEngine().startJourney(
   SpatialState(positions=mapOf("erik" to CharacterPosition("erik","ruins")),routes=listOf(Route("ruins","settlement",3))),
   "erik","settlement",12)
  assertEquals(3,state.journeys["erik"]?.remainingUnits)
  assertEquals(12,state.journeys["erik"]?.startedTurn)
 }
}