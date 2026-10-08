package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.*
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
 private fun remorsefulMind()=CharacterMind(personality=PersonalityProfile(mapOf(
  "honesty" to 80,"compassion" to 80,"responsibility" to 80
 )))

 @Test fun remorsefulOffscreenCharacterCanInitiateAcceptedRepair(){
  val (updated,cycle)=RelationshipRepairScheduler().run(story(remorsefulMind()),setOf("daniel"))
  val attempt=cycle.attempts.single()
  val hidden=updated.world.hiddenEvents.single{it.id=="hidden-"+attempt.id}
  val signal=updated.world.discovery.signals.getValue("social-trace-"+attempt.id)
  val consequence=updated.world.pendingConsequences.single{it.id=="repair-consequence-"+attempt.id}

  assertEquals(setOf(RepairAct.ADMISSION,RepairAct.APOLOGY,RepairAct.RESTITUTION),attempt.acts)
  assertTrue(cycle.outcomes.single().accepted)
  assertEquals(1,updated.world.socialKnowledge.relationshipRepairs.size)
  assertEquals(setOf("erik","elena"),hidden.knownBy)
  assertFalse("daniel" in hidden.knownBy)
  assertEquals(hidden.id,signal.relatedEventId)
  assertEquals("ruins",signal.locationId)
  assertFalse(signal.surface.contains("repair",ignoreCase=true))
  assertFalse(signal.surface.contains("apolog",ignoreCase=true))
  assertEquals(hidden.id,consequence.causeEventId)
  assertTrue(consequence.earliestTurn>updated.turn)
  assertFalse(consequence.description.contains("apolog",ignoreCase=true))
 }
 @Test fun observableTraceRequiresInvestigationBeforeItRevealsThePrivateEvent(){
  val (updated,cycle)=RelationshipRepairScheduler().run(story(remorsefulMind()),setOf("daniel"))
  val attempt=cycle.attempts.single()
  val hidden=updated.world.hiddenEvents.single{it.id=="hidden-"+attempt.id}
  val signal=updated.world.discovery.signals.getValue("social-trace-"+attempt.id)
  val discoveries=DiscoveryEngine()

  assertTrue(KnowledgeGate().visible(updated.world.hiddenEvents,"daniel").isEmpty())
  assertTrue(discoveries.visibleAt(updated.world.discovery,"daniel","settlement").isEmpty())
  assertEquals(listOf(signal),discoveries.visibleAt(updated.world.discovery,"daniel","ruins"))

  val investigated=assertNotNull(InvestigationEngine().investigate(
   updated.world.discovery,signal.id,"daniel",updated.world.characters.getValue("daniel").mind,13
  ))
  assertTrue(KnowledgeGate().visible(updated.world.hiddenEvents,"daniel").isEmpty())

  val revealed=KnowledgeGate().discover(
   updated.world.hiddenEvents,
   Discovery(hidden.id,"daniel",DiscoveryChannel.INVESTIGATION,"Their behavior suggests something changed.",investigated.evidence.id),
   DiscoverySources(evidence=investigated.state.evidence)
  )
  assertEquals(listOf(hidden.id),KnowledgeGate().visible(revealed,"daniel").map{it.id})
 }
 @Test fun onlyAParticipantCanDiscloseAndTheListenerMustActuallyHearIt(){
  val (updated,cycle)=RelationshipRepairScheduler().run(story(remorsefulMind()),setOf("daniel"))
  val attempt=cycle.attempts.single()
  val hiddenId="hidden-"+attempt.id
  val disclosureEngine=RelationshipRepairDisclosure()

  assertNull(disclosureEngine.disclose(updated,attempt.id,"daniel",13))
  val disclosure=assertNotNull(disclosureEngine.disclose(updated,attempt.id,"elena",13))
  assertEquals(hiddenId,disclosure.testimony.relatedEventId)
  assertEquals(TestimonyIntent.HONEST,disclosure.testimony.intent)
  assertSame(disclosure.testimony,disclosure.story.world.socialKnowledge.testimonies[disclosure.testimony.id])

  val discovery=Discovery(
   hiddenId,"daniel",DiscoveryChannel.TOLD,"Elena described their private exchange.",disclosure.testimony.id
  )
  val unheard=KnowledgeGate().discover(
   updated.world.hiddenEvents,
   discovery,
   DiscoverySources(testimonies=mapOf(disclosure.testimony.id to disclosure.testimony))
  )
  assertTrue(KnowledgeGate().visible(unheard,"daniel").isEmpty())

  val heard=SocialInformationEngine().hear(
   disclosure.testimony,"daniel",updated.world.characters.getValue("daniel").mind
  )
  val revealed=KnowledgeGate().discover(
   updated.world.hiddenEvents,
   discovery,
   DiscoverySources(
    testimonies=mapOf(disclosure.testimony.id to disclosure.testimony),
    heardClaims=mapOf(disclosure.testimony.id+"-daniel" to heard)
   )
  )
  assertEquals(listOf(hiddenId),KnowledgeGate().visible(revealed,"daniel").map{it.id})
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
  val separated=story(remorsefulMind()).let{base->
   base.copy(world=base.world.copy(spatial=base.world.spatial.copy(
    positions=base.world.spatial.positions+("elena" to CharacterPosition("elena","settlement"))
   )))
  }
  val (unchanged,cycle)=RelationshipRepairScheduler().run(separated,setOf("daniel"))
  assertTrue(cycle.attempts.isEmpty())
  assertTrue(unchanged.world.socialKnowledge.relationshipRepairs.isEmpty())
 }
 @Test fun protagonistPresenceKeepsRepairOutOfOffscreenScheduler(){
  val (_,cycle)=RelationshipRepairScheduler().run(story(remorsefulMind()),setOf("daniel","erik","elena"))
  assertTrue(cycle.attempts.isEmpty())
 }
}
