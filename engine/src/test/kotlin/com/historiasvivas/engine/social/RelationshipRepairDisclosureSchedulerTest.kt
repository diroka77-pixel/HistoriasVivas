package com.historiasvivas.engine.social

import com.historiasvivas.engine.character.CharacterMind
import com.historiasvivas.engine.character.PersonalityProfile
import com.historiasvivas.engine.model.Character
import com.historiasvivas.engine.model.Story
import com.historiasvivas.engine.model.WorldState
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.offscreen.KnowledgeGate
import com.historiasvivas.engine.spatial.CharacterPosition
import com.historiasvivas.engine.spatial.SpatialState
import kotlin.test.*

class RelationshipRepairDisclosureSchedulerTest {
 private val attemptId="repair-erik-elena-revision-1-12"
 private fun story(
  listenerLocation:String="ruins",
  openParticipants:Boolean=true
 ):Story{
  val honest=if(openParticipants)
   PersonalityProfile(mapOf("honesty" to 80,"loyalty" to 30,"compassion" to 40))
  else
   PersonalityProfile(mapOf("honesty" to 10,"secrecy" to 90,"caution" to 70))
  val reserved=PersonalityProfile(mapOf("honesty" to 10,"secrecy" to 80))
  val outcome=RelationshipRepairOutcome(
   attemptId,"erik","elena","revision-1",true,10,3,-2,45,12,"private outcome"
  )
  return Story(
   "s","Nadir","p",turn=13,protagonistId="daniel",
   world=WorldState(
    characters=mapOf(
     "erik" to Character("erik","Erik",mind=CharacterMind(personality=honest)),
     "elena" to Character("elena","Elena",mind=CharacterMind(personality=if(openParticipants)reserved else honest)),
     "daniel" to Character("daniel","Daniel")
    ),
    hiddenEvents=listOf(HiddenEvent(
     "hidden-"+attemptId,12,"erik","Erik made a limited repair with Elena",
     knownBy=setOf("erik","elena")
    )),
    spatial=SpatialState(positions=mapOf(
     "erik" to CharacterPosition("erik","ruins"),
     "elena" to CharacterPosition("elena","ruins"),
     "daniel" to CharacterPosition("daniel",listenerLocation)
    )),
    socialKnowledge=SocialKnowledgeState(relationshipRepairs=mapOf(attemptId to outcome))
   )
  )
 }

 @Test fun openParticipantCanVoluntarilyTellAPresentListener(){
  val original=story()
  val (updated,cycle)=RelationshipRepairDisclosureScheduler().run(original)
  val delivery=cycle.deliveries.single()
  val testimony=updated.world.socialKnowledge.testimonies.getValue(delivery.testimonyId)
  val heard=updated.world.socialKnowledge.heardClaims.getValue(delivery.testimonyId+":daniel")

  assertEquals("erik",delivery.speakerId)
  assertEquals("daniel",delivery.listenerId)
  assertEquals("hidden-"+attemptId,delivery.eventId)
  assertEquals(TestimonyIntent.HONEST,testimony.intent)
  assertEquals(delivery.eventId,testimony.relatedEventId)
  assertEquals(testimony.id,heard.testimonyId)
  assertEquals("daniel",heard.listenerId)
  assertTrue(updated.world.characters.getValue("daniel").mind.beliefs.any{
   it.sourceEventId==testimony.id
  })
  assertEquals(listOf(delivery.eventId),KnowledgeGate().visible(
   updated.world.hiddenEvents,"daniel"
  ).map{it.id})
 }
 @Test fun disclosureDoesNotReachAnAbsentListener(){
  val original=story(listenerLocation="settlement")
  val (unchanged,cycle)=RelationshipRepairDisclosureScheduler().run(original)

  assertSame(original,unchanged)
  assertTrue(cycle.deliveries.isEmpty())
  assertTrue(unchanged.world.socialKnowledge.testimonies.isEmpty())
  assertTrue(KnowledgeGate().visible(unchanged.world.hiddenEvents,"daniel").isEmpty())
 }
 @Test fun reservedParticipantsKeepThePrivateScenePrivate(){
  val original=story(openParticipants=false)
  val (unchanged,cycle)=RelationshipRepairDisclosureScheduler().run(original)

  assertSame(original,unchanged)
  assertTrue(cycle.deliveries.isEmpty())
  assertTrue(unchanged.world.socialKnowledge.heardClaims.isEmpty())
 }
 @Test fun theSamePrivateRepairIsNotDisclosedTwice(){
  val scheduler=RelationshipRepairDisclosureScheduler()
  val (once,first)=scheduler.run(story())
  val (same,second)=scheduler.run(once)

  assertEquals(1,first.deliveries.size)
  assertTrue(second.deliveries.isEmpty())
  assertEquals(once,same)
  assertEquals(1,same.world.socialKnowledge.testimonies.size)
  assertEquals(1,same.world.socialKnowledge.heardClaims.size)
 }
}
