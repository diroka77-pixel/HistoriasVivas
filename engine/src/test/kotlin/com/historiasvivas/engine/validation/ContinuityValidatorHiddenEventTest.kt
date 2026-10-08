package com.historiasvivas.engine.validation

import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.HiddenEvent
import com.historiasvivas.engine.turn.WorldDelta
import kotlin.test.*

class ContinuityValidatorHiddenEventTest {
 private fun story(existing:List<HiddenEvent> = emptyList())=Story(
  "s","Nadir","p",turn=4,
  world=WorldState(
   characters=mapOf(
    "erik" to Character("erik","Erik"),
    "elena" to Character("elena","Elena")
   ),
   hiddenEvents=existing
  )
 )
 private fun delta(vararg hidden:HiddenEvent)=WorldDelta(
  event=StoryEvent("turn-5",5,"The world advances"),
  hiddenEventsToAdd=hidden.toList()
 )
 private fun codes(story:Story,delta:WorldDelta)=
  ContinuityValidator().validate(story,delta).issues.map{it.code}.toSet()

 @Test fun validHiddenEventPassesContinuityValidation(){
  val hidden=HiddenEvent(
   "hidden-1",5,"erik","Erik acts privately",
   knownBy=setOf("erik","elena")
  )

  val result=ContinuityValidator().validate(story(),delta(hidden))
  assertTrue(result.accepted)
  assertTrue(result.issues.isEmpty())
 }
 @Test fun hiddenEventCannotReferenceUnknownCharacters(){
  val hidden=HiddenEvent(
   "hidden-1",5,"ghost","An unknown actor moves",
   knownBy=setOf("ghost"),discoveredBy=setOf("stranger")
  )

  val result=ContinuityValidator().validate(story(),delta(hidden))
  assertFalse(result.accepted)
  assertEquals(setOf("UNKNOWN_HIDDEN_CHARACTER"),result.issues.map{it.code}.toSet())
 }
 @Test fun hiddenEventMustBelongToAdvancingTurnAndBeKnownByItsActor(){
  val hidden=HiddenEvent(
   "hidden-1",7,"erik","Erik acts privately",
   knownBy=setOf("elena")
  )

  val found=codes(story(),delta(hidden))
  assertTrue("INVALID_HIDDEN_TURN" in found)
  assertTrue("HIDDEN_ACTOR_UNAWARE" in found)
 }
 @Test fun duplicateOrExistingHiddenIdsAreRejected(){
  val existing=HiddenEvent("existing",4,"erik","Earlier action")
  val first=HiddenEvent("duplicate",5,"erik","First action")
  val second=HiddenEvent("duplicate",5,"elena","Second action")
  val collision=HiddenEvent("existing",5,"elena","Conflicting action")

  val found=codes(story(listOf(existing)),delta(first,second,collision))
  assertTrue("DUPLICATE_HIDDEN_EVENT" in found)
  assertTrue("EXISTING_HIDDEN_EVENT" in found)
 }
 @Test fun hiddenEventsRequireMeaningfulIdentityAndContent(){
  val blank=HiddenEvent("",5,"erik"," ")

  assertTrue("INVALID_HIDDEN_CONTENT" in codes(story(),delta(blank)))
 }
}
