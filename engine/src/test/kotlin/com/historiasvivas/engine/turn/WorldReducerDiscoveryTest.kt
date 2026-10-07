package com.historiasvivas.engine.turn

import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.world.*
import kotlin.test.*

class WorldReducerDiscoveryTest {
 @Test fun strongerWorldPressureReactivatesIgnoredPersistentSignal(){
  val old=WorldSignal(
   id="pressure-fauna",locationId=null,sense=SignalSense.ENVIRONMENT,
   surface="Unusual tracks and disturbed ground suggest increased animal activity",
   hiddenCause="pressure:fauna:50",createdTurn=4,salience=50,persistent=true,
   noticedBy=setOf("daniel"),ignoredBy=setOf("daniel"),lastChangedTurn=4
  )
  val story=Story(
   id="s",title="Nadir",premise="p",turn=4,
   world=WorldState(
    environment=EnvironmentState(pressures=mapOf("fauna" to WorldPressure("fauna",50,10,""))),
    discovery=DiscoveryState(signals=mapOf(old.id to old))
   )
  )
  val event=StoryEvent("player-5",5,"Daniel waits")
  val next=WorldReducer().apply(story,WorldDelta(event=event))
  val evolved=next.world.discovery.signals.getValue(old.id)
  assertEquals(1,evolved.revision)
  assertEquals(60,evolved.salience)
  assertEquals(5,evolved.lastChangedTurn)
  assertEquals(1,next.world.discovery.signals.keys.count{it=="pressure-fauna"})
  assertEquals(listOf("pressure-fauna"),DiscoveryEngine().visibleAt(next.world.discovery,"daniel",null).map{it.id})
 }
 @Test fun progressingJourneyReactivatesIgnoredTraceWithoutDuplicatingIt(){
  val trace=WorldSignal(
   id="trace-erik-2",locationId=null,routeFrom="settlement",routeTo="ruins",
   sense=SignalSense.TRACE,surface="Fresh signs show that someone has just entered the route",
   hiddenCause="journey:erik:settlement:ruins:elapsed=0",createdTurn=2,salience=55,persistent=true,
   noticedBy=setOf("daniel"),ignoredBy=setOf("daniel"),lastChangedTurn=2
  )
  val spatial=com.historiasvivas.engine.spatial.SpatialState(
   positions=mapOf("erik" to com.historiasvivas.engine.spatial.CharacterPosition("erik","settlement",2)),
   journeys=mapOf("erik" to com.historiasvivas.engine.spatial.Journey("erik","settlement","ruins",3,0,2))
  )
  val story=Story("s","Nadir","p",world=WorldState(spatial=spatial,discovery=DiscoveryState(signals=mapOf(trace.id to trace))),turn=2)
  val next=WorldReducer().apply(story,WorldDelta(event=StoryEvent("player-3",3,"Daniel waits")))
  val evolved=next.world.discovery.signals.getValue(trace.id)
  assertEquals(1,evolved.revision)
  assertEquals(60,evolved.salience)
  assertEquals(3,evolved.lastChangedTurn)
  assertEquals("journey:erik:settlement:ruins:elapsed=1",evolved.hiddenCause)
  assertEquals(1,next.world.discovery.signals.keys.count{it==trace.id})
  assertEquals(listOf(trace.id),DiscoveryEngine().visibleOnJourney(next.world.discovery,"daniel","settlement","ruins").map{it.id})
 }

}
