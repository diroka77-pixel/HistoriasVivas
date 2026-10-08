package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.spatial.*
import kotlin.test.*

class RelationshipRepairSchedulerTest {
 private fun revision()=BeliefRevision(
  id="revision-1",
  previous=Belief("erik:t1","The ruins are empty",70,"t1"),
  revised=Belief("erik:t1","The ruins are empty",0,"t1",status=BeliefStatus.REJECTED),
  confirmed=Belief("erik:t1","Erik entered the ruins",90,"e1"),
  evidenceId="e1",
  sourceCharacterId="erik",
  turn=10
 )
 private fun story(offenderMind:CharacterMind,harmedMind:CharacterMind=CharacterMind(
  personality=PersonalityProfile(mapOf("compassion" to 60,"patience" to 60)),
  beliefRevisions=listOf(revision())
 ))=Story(
  "s","Nadir","p",turn=12,protagonistId="daniel",
  world=WorldState(
   characters=mapOf(
    "erik" to Character("erik","Erik",mind=offenderMind),
    "elena" to Character("elena","Elena",mind=harmedMind),
    "daniel" to Character("daniel","Daniel")
   ),
   relationships=listOf(
    Relationship("erik","elena",trust=20,affection=40),
    Relationship("elena","erik",trust=-40,affection=15)
   ),
   spatial=SpatialState(positions=mapOf(
    "erik" to CharacterPosition("erik","ruins"),
    "elena" to CharacterPosition("elena","ruins"),
    "daniel" to CharacterPosition("daniel","settlement")
   ))
  )
 )

 @Test fun remorsefulOffscreenCharacterCanInitiateAcceptedRepair(){
  val offenderMind=CharacterMind(personality=PersonalityProfile(mapOf(
   "honesty" to 80,"compassion" to 80,"responsibility" to 80
  )))
  val (updated,cycle)=RelationshipRepairScheduler().run(story(offenderMind),setOf("daniel"))
  assertEquals(1,cycle.attempts.size)
  assertEquals(setOf(RepairAct.ADMISSION,RepairAct.APOLOGY,RepairAct.RESTITUTION),cycle.attempts.single().acts)
  assertTrue(cycle.outcomes.single().accepted)
  assertEquals(1,updated.world.socialKnowledge.relationshipRepairs.size)
  assertTrue(updated.world.hiddenEvents.any{it.id=="hidden-"+cycle.attempts.single().id})
  assertTrue(updated.world.hiddenEvents.single{it.id=="hidden-"+cycle.attempts.single().id}.knownBy.containsAll(setOf("erik","elena")))
 }
 @Test fun rejectedRepairObservesCooldownBeforeTryingAgain(){
  val guardedMind=CharacterMind(personality=PersonalityProfile(mapOf("honesty" to 0,"pride" to 80)))
  val cautiousHarmed=CharacterMind(
   personality=PersonalityProfile(mapOf("caution" to 90)),
   beliefRevisions=listOf(revision())
  )
  val scheduler=RelationshipRepairScheduler()
  val (afterFirst,first)=scheduler.run(story(guardedMind,cautiousHarmed),setOf("daniel"))
  assertEquals(1,first.outcomes.size)
  assertFalse(first.outcomes.single().accepted)

  val (tooSoon,second)=scheduler.run(afterFirst.copy(turn=13),setOf("daniel"))
  assertTrue(second.attempts.isEmpty())
  assertEquals(afterFirst.world.socialKnowledge.relationshipRepairs,tooSoon.world.socialKnowledge.relationshipRepairs)

  val (_,later)=scheduler.run(afterFirst.copy(turn=17),setOf("daniel"))
  assertEquals(1,later.attempts.size)
 }
 @Test fun repairRequiresARealMeeting(){
  val offenderMind=CharacterMind(personality=PersonalityProfile(mapOf("honesty" to 80,"compassion" to 80)))
  val separated=story(offenderMind).let{base->
   base.copy(world=base.world.copy(spatial=base.world.spatial.copy(
    positions=base.world.spatial.positions+("elena" to CharacterPosition("elena","settlement"))
   )))
  }
  val (unchanged,cycle)=RelationshipRepairScheduler().run(separated,setOf("daniel"))
  assertTrue(cycle.attempts.isEmpty())
  assertTrue(unchanged.world.socialKnowledge.relationshipRepairs.isEmpty())
 }
 @Test fun protagonistPresenceKeepsRepairOutOfOffscreenScheduler(){
  val offenderMind=CharacterMind(personality=PersonalityProfile(mapOf("honesty" to 80,"compassion" to 80)))
  val (_,cycle)=RelationshipRepairScheduler().run(story(offenderMind),setOf("daniel","erik","elena"))
  assertTrue(cycle.attempts.isEmpty())
 }
}
