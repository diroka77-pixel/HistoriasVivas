package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.CharacterMind
import com.historiasvivas.engine.character.PersonalityProfile
import com.historiasvivas.engine.model.Character
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.model.WorldState
import com.historiasvivas.engine.offscreen.*
import com.historiasvivas.engine.spatial.CharacterPosition
import com.historiasvivas.engine.spatial.SpatialState
import kotlin.test.*

class RumorPropagationSchedulerTest {
 private val eventId="hidden-repair-1"
 private val claim="Erik admitted what happened, and Elena accepted a limited repair."
 private fun openMind()=CharacterMind(personality=PersonalityProfile(mapOf(
  "honesty" to 80,"compassion" to 50,"loyalty" to 40
 )))
 private fun story(listenerLocation:String="ruins",openSpeaker:Boolean=true):Story{
  val direct=Testimony(
   "repair-testimony-1","erik",claim,"elena",12,
   channel=InformationChannel.DIRECT_TESTIMONY,
   intent=TestimonyIntent.HONEST,reliability=80,relatedEventId=eventId
  )
  val heard=HeardClaim(
   direct.id,"elena","erik",claim,80,12,InformationChannel.DIRECT_TESTIMONY,eventId
  )
  val elenaMind=if(openSpeaker)openMind() else CharacterMind(
   personality=PersonalityProfile(mapOf("honesty" to 10,"secrecy" to 90,"caution" to 70))
  )
  return Story(
   "s","Nadir","p",turn=13,protagonistId="daniel",
   world=WorldState(
    characters=mapOf(
     "erik" to Character("erik","Erik"),
     "elena" to Character("elena","Elena",mind=elenaMind),
     "daniel" to Character("daniel","Daniel",mind=openMind())
    ),
    hiddenEvents=listOf(HiddenEvent(
     eventId,12,"erik","A private repair occurred",
     knownBy=setOf("erik"),discoveredBy=setOf("elena")
    )),
    spatial=SpatialState(positions=mapOf(
     "erik" to CharacterPosition("erik","settlement"),
     "elena" to CharacterPosition("elena","ruins"),
     "daniel" to CharacterPosition("daniel",listenerLocation)
    )),
    socialKnowledge=SocialKnowledgeState(
     testimonies=mapOf(direct.id to direct),
     heardClaims=mapOf(direct.id+":elena" to heard)
    )
   )
  )
 }

 @Test fun presentCharacterCanRelayAWeakerRumor(){
  val original=story()
  val (updated,cycle)=RumorPropagationScheduler().run(original)
  val delivery=cycle.deliveries.single()
  val testimony=updated.world.socialKnowledge.testimonies.getValue(delivery.testimonyId)
  val heard=updated.world.socialKnowledge.heardClaims.getValue(testimony.id+":daniel")

  assertEquals("elena",delivery.speakerId)
  assertEquals("daniel",delivery.listenerId)
  assertEquals(65,delivery.confidence)
  assertEquals(InformationChannel.RUMOR,testimony.channel)
  assertEquals(TestimonyIntent.UNCERTAIN,testimony.intent)
  assertEquals(setOf("repair-testimony-1"),testimony.sourceEvidenceIds)
  assertEquals(eventId,testimony.relatedEventId)
  assertEquals(65,heard.confidence)
  assertTrue(updated.world.characters.getValue("daniel").mind.beliefs.any{
   it.sourceEventId==testimony.id&&it.confidence==65
  })
  assertTrue(KnowledgeGate().visible(updated.world.hiddenEvents,"daniel").isEmpty())
 }
 @Test fun rumorNeverConfirmsTheHiddenEvent(){
  val (updated,cycle)=RumorPropagationScheduler().run(story())
  val testimonyId=cycle.deliveries.single().testimonyId
  val attempted=KnowledgeGate().discover(
   updated.world.hiddenEvents,
   Discovery(eventId,"daniel",DiscoveryChannel.RUMOR,"Elena says it happened.",testimonyId),
   DiscoverySources(
    testimonies=updated.world.socialKnowledge.testimonies,
    heardClaims=updated.world.socialKnowledge.heardClaims
   )
  )

  assertEquals(updated.world.hiddenEvents,attempted)
  assertTrue(KnowledgeGate().visible(attempted,"daniel").isEmpty())
 }
 @Test fun rumorRequiresPresenceAndAWillingSpeaker(){
  val absent=story(listenerLocation="settlement")
  val (unchangedAbsent,absentCycle)=RumorPropagationScheduler().run(absent)
  assertSame(absent,unchangedAbsent)
  assertTrue(absentCycle.deliveries.isEmpty())

  val reserved=story(openSpeaker=false)
  val (unchangedReserved,reservedCycle)=RumorPropagationScheduler().run(reserved)
  assertSame(reserved,unchangedReserved)
  assertTrue(reservedCycle.deliveries.isEmpty())
 }
 @Test fun eachRelayHappensOnceAndAChainKeepsDegrading(){
  val scheduler=RumorPropagationScheduler()
  val (first,firstCycle)=scheduler.run(story())
  val (same,duplicateCycle)=scheduler.run(first)
  assertEquals(1,firstCycle.deliveries.size)
  assertTrue(duplicateCycle.deliveries.isEmpty())
  assertEquals(first,same)

  val withNora=first.copy(
   turn=14,
   world=first.world.copy(
    characters=first.world.characters+("nora" to Character("nora","Nora")),
    spatial=first.world.spatial.copy(
     positions=first.world.spatial.positions+
      ("nora" to CharacterPosition("nora","ruins"))
    )
   )
  )
  val (second,secondCycle)=scheduler.run(withNora)
  val relayed=secondCycle.deliveries.single()
  assertEquals("daniel",relayed.speakerId)
  assertEquals("nora",relayed.listenerId)
  assertEquals(50,relayed.confidence)
  assertTrue(relayed.confidence<firstCycle.deliveries.single().confidence)
  assertTrue(KnowledgeGate().visible(second.world.hiddenEvents,"nora").isEmpty())
 }
}
