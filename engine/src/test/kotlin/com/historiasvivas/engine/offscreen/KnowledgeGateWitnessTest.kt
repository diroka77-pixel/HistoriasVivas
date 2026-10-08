package com.historiasvivas.engine.offscreen

import kotlin.test.*

class KnowledgeGateWitnessTest {
 private val hidden=HiddenEvent(
  "hidden-1",5,"erik","Erik acts privately",
  knownBy=setOf("erik","elena")
 )

 @Test fun outsiderCannotDeclareThatTheyWitnessedAHiddenEvent(){
  val events=listOf(hidden)
  val attempted=KnowledgeGate().discover(
   events,
   Discovery(hidden.id,"daniel",DiscoveryChannel.WITNESSED,"I saw it happen")
  )

  assertEquals(events,attempted)
  assertTrue(KnowledgeGate().visible(attempted,"daniel").isEmpty())
 }
 @Test fun registeredWitnessCanUseTheWitnessedChannel(){
  val discovered=KnowledgeGate().discover(
   listOf(hidden),
   Discovery(hidden.id,"elena",DiscoveryChannel.WITNESSED,"I saw it happen")
  )

  val event=discovered.single()
  assertTrue("elena" in event.discoveredBy)
  assertEquals(listOf(hidden.id),KnowledgeGate().visible(discovered,"elena").map{it.id})
 }
 @Test fun witnessedDiscoveryRejectsInventedSourceIds(){
  val events=listOf(hidden)
  val attempted=KnowledgeGate().discover(
   events,
   Discovery(hidden.id,"elena",DiscoveryChannel.WITNESSED,"I saw it happen","fake-source")
  )

  assertEquals(events,attempted)
  assertFalse("elena" in attempted.single().discoveredBy)
 }
 @Test fun blankInterpretationAndUnknownEventCannotPass(){
  val gate=KnowledgeGate()
  val blank=gate.discover(
   listOf(hidden),
   Discovery(hidden.id,"elena",DiscoveryChannel.WITNESSED," ")
  )
  val missing=gate.discover(
   listOf(hidden),
   Discovery("missing","elena",DiscoveryChannel.WITNESSED,"I saw it happen")
  )

  assertEquals(listOf(hidden),blank)
  assertEquals(listOf(hidden),missing)
 }
}
