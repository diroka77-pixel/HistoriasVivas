package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.*
import com.historiasvivas.engine.model.Character
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.model.WorldState
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.offscreen.KnowledgeGate
import com.historiasvivas.engine.spatial.CharacterPosition
import com.historiasvivas.engine.spatial.SpatialState
import kotlin.test.*

class RumorInvestigationSchedulerTest {
 private val eventId="hidden-repair-1"
 private fun story(
  signalEventId:String?=eventId,
  signalLocation:String="ruins",
  heardTurn:Long=10
 )=Story(
  "s","Nadir","p",turn=11,protagonistId="daniel",
  world=WorldState(
   characters=mapOf(
    "elena" to Character("elena","Elena",mind=CharacterMind()),
    "daniel" to Character("daniel","Daniel")
   ),
   hiddenEvents=listOf(HiddenEvent(
    eventId,9,"erik","A private repair occurred",knownBy=setOf("erik")
   )),
   spatial=SpatialState(positions=mapOf(
    "elena" to CharacterPosition("elena","ruins"),
    "daniel" to CharacterPosition("daniel","settlement")
   )),
   discovery=DiscoveryState(signals=mapOf(
    "trace-1" to WorldSignal(
     "trace-1",signalLocation,sense=SignalSense.SIGHT,
     surface="Two people coordinate with unusual care.",
     createdTurn=10,salience=60,relatedEventId=signalEventId
    )
   )),
   socialKnowledge=SocialKnowledgeState(heardClaims=mapOf(
    "rumor-1:elena" to HeardClaim(
     "rumor-1","elena","nora","Something changed between them",60,heardTurn,
     InformationChannel.RUMOR,eventId
    )
   ))
  )
 )

 @Test fun offscreenCharacterCanGroundRumorByInvestigatingMatchingLocalSignal(){
  val original=story()
  val (updated,cycle)=RumorInvestigationScheduler().run(
   original,excludedCharacterIds=setOf("daniel")
  )
  val completed=cycle.investigations.single()
  val evidence=updated.world.discovery.evidence.getValue(completed.evidenceId)

  assertEquals("elena",completed.characterId)
  assertEquals(eventId,completed.eventId)
  assertEquals("trace-1",completed.signalId)
  assertEquals(eventId,evidence.relatedEventId)
  assertEquals("elena",evidence.discoveredBy)
  assertEquals(listOf(eventId),KnowledgeGate().visible(
   updated.world.hiddenEvents,"elena"
  ).map{it.id})
  assertTrue(KnowledgeGate().visible(updated.world.hiddenEvents,"daniel").isEmpty())
 }
 @Test fun investigationRequiresAnExactSignalAtTheCharactersLocation(){
  val unrelated=story(signalEventId="another-event")
  val (sameUnrelated,unrelatedCycle)=RumorInvestigationScheduler().run(unrelated)
  assertSame(unrelated,sameUnrelated)
  assertTrue(unrelatedCycle.investigations.isEmpty())

  val distant=story(signalLocation="settlement")
  val (sameDistant,distantCycle)=RumorInvestigationScheduler().run(distant)
  assertSame(distant,sameDistant)
  assertTrue(distantCycle.investigations.isEmpty())
 }
 @Test fun presentOrJustInformedCharacterIsNotAdvancedOffscreen(){
  val original=story()
  val (excluded,excludedCycle)=RumorInvestigationScheduler().run(
   original,excludedCharacterIds=setOf("elena")
  )
  assertSame(original,excluded)
  assertTrue(excludedCycle.investigations.isEmpty())

  val justHeard=story(heardTurn=11)
  val (same,cycle)=RumorInvestigationScheduler().run(justHeard)
  assertSame(justHeard,same)
  assertTrue(cycle.investigations.isEmpty())
 }
 @Test fun groundedRumorIsNotInvestigatedTwice(){
  val scheduler=RumorInvestigationScheduler()
  val (once,first)=scheduler.run(story())
  val (same,second)=scheduler.run(once)

  assertEquals(1,first.investigations.size)
  assertTrue(second.investigations.isEmpty())
  assertEquals(once,same)
  assertEquals(1,same.world.discovery.evidence.size)
 }
 @Test fun independentEvidenceRevisesTheRumorWithoutBlamingACharacter(){
  val base=story()
  val heard=base.world.socialKnowledge.heardClaims.getValue("rumor-1:elena")
  val rumor=Testimony(
   heard.testimonyId,"nora",heard.claim,"erik",heard.turn,
   channel=InformationChannel.RUMOR,intent=TestimonyIntent.UNCERTAIN,
   reliability=heard.confidence,relatedEventId=eventId
  )
  val oldBelief=SocialInformationEngine().asBelief(heard)
  val primed=base.copy(world=base.world.copy(
   characters=base.world.characters+(
    "elena" to base.world.characters.getValue("elena").copy(
     mind=base.world.characters.getValue("elena").mind.copy(beliefs=listOf(oldBelief))
    )
   ),
   socialKnowledge=base.world.socialKnowledge.copy(
    testimonies=mapOf(rumor.id to rumor)
   )
  ))

  val (updated,cycle)=RumorInvestigationScheduler().run(primed)
  val evidenceId=cycle.investigations.single().evidenceId
  val mind=updated.world.characters.getValue("elena").mind
  val revisedRumor=mind.beliefs.single{it.sourceEventId==rumor.id}
  val grounded=mind.beliefs.single{it.sourceEventId==evidenceId}
  val revision=mind.beliefRevisions.single()

  assertEquals(BeliefStatus.CONTESTED,revisedRumor.status)
  assertEquals(evidenceId,revisedRumor.supersededBySourceId)
  assertEquals(BeliefStatus.ACTIVE,grounded.status)
  assertEquals(evidenceId,revision.evidenceId)
  assertEquals(grounded,revision.confirmed)
  assertNull(revision.sourceCharacterId)
 }

}
