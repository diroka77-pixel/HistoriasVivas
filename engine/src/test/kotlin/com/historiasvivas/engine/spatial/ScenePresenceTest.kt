package com.historiasvivas.engine.spatial

import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.turn.WorldDelta
import com.historiasvivas.engine.turn.WorldReducer
import kotlin.test.*

class ScenePresenceTest {
 @Test fun completedJourneyChangesWhoIsPresentWithTheProtagonist(){
  val story=Story(
   "s","Nadir","p",turn=4,protagonistId="daniel",
   world=WorldState(
    characters=mapOf(
     "daniel" to Character("daniel","Daniel"),
     "erik" to Character("erik","Erik")
    ),
    spatial=SpatialState(
     positions=mapOf(
      "daniel" to CharacterPosition("daniel","ruins",1),
      "erik" to CharacterPosition("erik","settlement",1)
     ),
     routes=listOf(Route("settlement","ruins",1)),
     journeys=mapOf(
      "erik" to Journey("erik","settlement","ruins",1,0,4)
     )
    )
   )
  )
  val presence=ScenePresence()
  assertEquals(setOf("daniel"),presence.withProtagonist(story))

  val advanced=WorldReducer().apply(
   story,WorldDelta(event=StoryEvent("turn-5",5,"Daniel waits"))
  )

  assertEquals(setOf("daniel","erik"),presence.withProtagonist(advanced))
  assertFalse("erik" in advanced.world.spatial.journeys)
  assertEquals("ruins",advanced.world.spatial.positions.getValue("erik").locationId)
 }
 @Test fun travellingProtagonistFallsBackToOnlyTheProtagonist(){
  val story=Story(
   "s","Nadir","p",protagonistId="daniel",
   world=WorldState(
    characters=mapOf(
     "daniel" to Character("daniel","Daniel"),
     "erik" to Character("erik","Erik")
    ),
    spatial=SpatialState(
     positions=mapOf(
      "daniel" to CharacterPosition("daniel","settlement"),
      "erik" to CharacterPosition("erik","settlement")
     ),
     journeys=mapOf(
      "daniel" to Journey("daniel","settlement","ruins",2)
     )
    )
   )
  )

  assertEquals(setOf("daniel"),ScenePresence().withProtagonist(story))
 }
 @Test fun presenceNeverIncludesUnknownSpatialEntities(){
  val story=Story(
   "s","Nadir","p",protagonistId="daniel",
   world=WorldState(
    characters=mapOf("daniel" to Character("daniel","Daniel")),
    spatial=SpatialState(positions=mapOf(
     "daniel" to CharacterPosition("daniel","ruins"),
     "ghost" to CharacterPosition("ghost","ruins")
    ))
   )
  )

  assertEquals(setOf("daniel"),ScenePresence().withProtagonist(story))
 }
}
