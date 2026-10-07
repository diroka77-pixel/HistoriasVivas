package com.historiasvivas.engine.spatial
import com.historiasvivas.engine.model.*
import kotlin.test.*
class JourneyDiversionTest {
 @Test fun diversionReplacesDestinationInsteadOfReturningToOriginalPath(){
  val engine=SpatialEngine()
  var state=SpatialState(positions=mapOf("daniel" to CharacterPosition("daniel","settlement")),routes=listOf(Route("settlement","ruins",3)))
  state=engine.startJourney(state,"daniel","ruins",1)
  state=engine.advanceJourneys(state,1,2)
  state=engine.divertJourney(state,"daniel","forest-depths",4,2,"Followed unknown tracks")
  val journey=state.journeys["daniel"]!!
  assertEquals("ruins",journey.originalDestination)
  assertEquals("forest-depths",journey.to)
  assertEquals("ruins",journey.diversions.single().abandonedDestination)
  state=engine.advanceJourneys(state,4,6)
  assertEquals("forest-depths",engine.locationOf(state,"daniel"))
 }
 @Test fun abandonedDestinationCreatesFutureCausalOpportunity(){
  val diversion=JourneyDiversion(5,"ruins","forest-depths","Followed tracks")
  val journey=Journey("daniel","settlement","forest-depths",5,1,1,"ruins",listOf(diversion))
  val effect=JourneyCausality().fromDiversion(Story("s","Nadir","p"),journey,diversion)
  assertTrue(effect.consequence.description.contains("ruins"))
  assertEquals(6,effect.consequence.earliestTurn)
 }
}