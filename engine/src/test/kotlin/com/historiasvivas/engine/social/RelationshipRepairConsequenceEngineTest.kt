package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.CharacterMind
import com.historiasvivas.engine.discovery.DiscoveryEngine
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.spatial.CharacterPosition
import com.historiasvivas.engine.spatial.SpatialState
import kotlin.test.*

class RelationshipRepairConsequenceEngineTest {
 private fun story(
  accepted:Boolean=true,
  turn:Long=13,
  earliestTurn:Long=13,
  together:Boolean=true
 ):Story{
  val attemptId="repair-erik-elena-revision-1-12"
  val outcome=RelationshipRepairOutcome(
   attemptId,"erik","elena","revision-1",accepted,
   if(accepted)10 else 0,if(accepted)3 else 0,if(accepted)-2 else 0,
   if(accepted)45 else 100,12,"private outcome"
  )
  return Story(
   id="s",title="Nadir",premise="p",turn=turn,protagonistId="daniel",
   world=WorldState(
    characters=mapOf(
     "erik" to Character("erik","Erik",mind=CharacterMind()),
     "elena" to Character("elena","Elena",mind=CharacterMind()),
     "daniel" to Character("daniel","Daniel",mind=CharacterMind())
    ),
    relationships=listOf(Relationship("elena","erik",trust=-20,affection=5,fear=10)),
    pendingConsequences=listOf(Consequence(
     "repair-consequence-"+attemptId,
     "hidden-"+attemptId,
     "Future behavior may change.",
     earliestTurn
    )),
    hiddenEvents=listOf(HiddenEvent(
     "hidden-"+attemptId,12,"erik","A private repair occurred",knownBy=setOf("erik","elena")
    )),
    spatial=SpatialState(positions=mapOf(
     "erik" to CharacterPosition("erik","ruins"),
     "elena" to CharacterPosition("elena",if(together)"ruins" else "settlement"),
     "daniel" to CharacterPosition("daniel","settlement")
    )),
    socialKnowledge=SocialKnowledgeState(relationshipRepairs=mapOf(attemptId to outcome))
   )
  )
 }

 @Test fun acceptedRepairChangesLaterConductAtTheNextRealMeeting(){
  val original=story()
  val (updated,cycle)=RelationshipRepairConsequenceEngine().resolve(original)
  val consequence=updated.world.pendingConsequences.single()
  val relationship=updated.world.relationships.single()
  val signal=updated.world.discovery.signals.getValue(cycle.generatedSignalIds.single())

  assertEquals(listOf(consequence.id),cycle.resolvedConsequenceIds)
  assertTrue(consequence.resolved)
  assertEquals(-18,relationship.trust)
  assertEquals(6,relationship.affection)
  assertEquals(9,relationship.fear)
  assertEquals(2,updated.world.characters.getValue("elena").mind.perceptions.getValue("erik").impressions["trust"])
  assertEquals("ruins",signal.locationId)
  assertEquals(original.world.hiddenEvents.single().id,signal.relatedEventId)
  assertFalse(signal.surface.contains("repair",ignoreCase=true))
  assertFalse(signal.surface.contains("apolog",ignoreCase=true))
  assertTrue(DiscoveryEngine().visibleAt(updated.world.discovery,"daniel","settlement").isEmpty())
  assertEquals(listOf(signal.id),DiscoveryEngine().visibleAt(updated.world.discovery,"daniel","ruins").map{it.id})
 }
 @Test fun rejectedRepairProducesDistanceAndDeepensDistrust(){
  val (updated,cycle)=RelationshipRepairConsequenceEngine().resolve(story(accepted=false))
  val relationship=updated.world.relationships.single()
  val signal=updated.world.discovery.signals.getValue(cycle.generatedSignalIds.single())

  assertEquals(-23,relationship.trust)
  assertEquals(4,relationship.affection)
  assertEquals(11,relationship.fear)
  assertTrue(signal.surface.contains("distance",ignoreCase=true))
  assertFalse(signal.surface.contains("repair",ignoreCase=true))
  assertTrue(updated.world.pendingConsequences.single().resolved)
 }
 @Test fun consequenceWaitsUntilItsEarliestTurn(){
  val original=story(turn=12,earliestTurn=13)
  val (unchanged,cycle)=RelationshipRepairConsequenceEngine().resolve(original)

  assertSame(original,unchanged)
  assertTrue(cycle.resolvedConsequenceIds.isEmpty())
  assertFalse(unchanged.world.pendingConsequences.single().resolved)
 }
 @Test fun consequenceWaitsForARealEncounterAndResolvesOnlyOnce(){
  val apart=story(together=false)
  val (waiting,waitingCycle)=RelationshipRepairConsequenceEngine().resolve(apart)
  assertTrue(waitingCycle.resolvedConsequenceIds.isEmpty())
  assertFalse(waiting.world.pendingConsequences.single().resolved)

  val reunited=waiting.copy(world=waiting.world.copy(spatial=waiting.world.spatial.copy(
   positions=waiting.world.spatial.positions+("elena" to CharacterPosition("elena","ruins"))
  )))
  val engine=RelationshipRepairConsequenceEngine()
  val (resolved,firstCycle)=engine.resolve(reunited)
  val (same,secondCycle)=engine.resolve(resolved)

  assertEquals(1,firstCycle.resolvedConsequenceIds.size)
  assertTrue(secondCycle.resolvedConsequenceIds.isEmpty())
  assertEquals(resolved,same)
  assertEquals(1,same.world.discovery.signals.keys.count{it.startsWith("repair-follow-through-")})
 }
}
