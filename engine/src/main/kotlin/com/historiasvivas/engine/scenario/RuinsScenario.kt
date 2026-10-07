package com.historiasvivas.engine.scenario
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.world.*

object RuinsScenario {
 fun create():Story {
  val daniel=Character("daniel","Daniel",setOf("protagonist","curious"),listOf("Keep the settlement alive","Understand the ruins"),
   mind=CharacterMind(PersonalityProfile(mapOf("compassion" to 55,"courage" to 45,"curiosity" to 70))))
  val elena=Character("elena","Elena",setOf("medic","observant"),listOf("Protect the colony","Discover what the council hides"),
   mind=CharacterMind(PersonalityProfile(mapOf("compassion" to 70,"caution" to 65,"loyalty" to 50)),
    beliefs=listOf(Belief("ruins","The ruins react to human presence",65))))
  val erik=Character("erik","Erik",setOf("engineer","restless"),listOf("Restore the old transmitter","Prove the ruins are not abandoned"),
   mind=CharacterMind(PersonalityProfile(mapOf("courage" to 75,"curiosity" to 80,"patience" to -20)),
    beliefs=listOf(Belief("ruins","Something intelligent is watching the colony",72))))
  return Story(
   id="lab-ruins",title="Las ruinas de Nadir",
   premise="Humanity abandoned Earth and the survivors founded Nadir on an unknown planet containing ruins of an earlier civilization.",
   world=WorldState(
    characters=mapOf(daniel.id to daniel,elena.id to elena,erik.id to erik),
    locations=mapOf(
     "settlement" to Location("settlement","Nadir",setOf("Human settlement","Resources are limited")),
     "ruins" to Location("ruins","The Northern Ruins",setOf("Older than the colony","Origin unknown")),
     "forest" to Location("forest","Black Forest",setOf("Dangerous native fauna"))
    ),
    relationships=listOf(
     Relationship("elena","daniel",trust=20,affection=10),
     Relationship("erik","daniel",trust=15),
     Relationship("elena","erik",trust=5,fear=5)
    ),
    secrets=listOf(
     Secret("signal","A repeating signal originates beneath the ruins",knownBy=setOf("erik")),
     Secret("council","The council detected the signal before landing",knownBy=emptySet())
    ),
    environment=EnvironmentState(
     resources=mapOf("water" to ResourcePool("water",65,100,18),"food" to ResourcePool("food",55,100,15),"energy" to ResourcePool("energy",70,100,20)),
     pressures=mapOf("fauna" to WorldPressure("fauna",20,1,"Native fauna is probing the settlement perimeter"),"ruins" to WorldPressure("ruins",10,0,"Unexplained activity around the ruins"))
    ),
    facts=setOf(
     "Earth is no longer habitable",
     "Nadir is the survivors' settlement",
     "The Northern Ruins predate human arrival",
     "The Black Forest contains dangerous native fauna"
    )
   )
  )
 }
}
