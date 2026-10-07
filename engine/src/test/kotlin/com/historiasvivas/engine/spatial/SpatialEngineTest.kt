package com.historiasvivas.engine.spatial
import kotlin.test.*
class SpatialEngineTest {
 @Test fun charactersRemainWhereWorldPlacedThem(){
  val state=SpatialState(
   positions=mapOf("daniel" to CharacterPosition("daniel","settlement"),"erik" to CharacterPosition("erik","ruins")),
   routes=listOf(Route("settlement","ruins",3))
  )
  val engine=SpatialEngine()
  assertEquals(setOf("daniel"),engine.presentAt(state,"settlement"))
  assertEquals("ruins",engine.locationOf(state,"erik"))
  assertEquals(3,engine.travelUnits(state,"settlement","ruins"))
 }
 @Test fun blockedOrNonexistentRoutePreventsTeleportation(){
  val state=SpatialState(
   positions=mapOf("erik" to CharacterPosition("erik","ruins")),
   routes=listOf(Route("ruins","forest",2,blocked=true))
  )
  val moved=SpatialEngine().move(state,"erik","forest",10)
  assertEquals("ruins",moved.positions["erik"]?.locationId)
 }
}