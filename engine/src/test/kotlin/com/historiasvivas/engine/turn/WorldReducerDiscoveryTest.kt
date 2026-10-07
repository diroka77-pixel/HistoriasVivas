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
}
