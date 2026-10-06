package com.historiasvivas.engine
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.offscreen.*
import kotlin.test.*

class OffscreenWorldTest {
 @Test fun protagonistDoesNotAutomaticallyKnowOffscreenEvents(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val gate=KnowledgeGate()
  assertTrue(gate.visible(listOf(event),"daniel").isEmpty())
  assertEquals(1,gate.visible(listOf(event),"erik").size)
 }
 @Test fun evidenceCanRevealHiddenEventLater(){
  val event=HiddenEvent("h1",5,"erik","Erik entered the ruins")
  val gate=KnowledgeGate()
  val updated=gate.discover(listOf(event),Discovery("h1","daniel",DiscoveryChannel.EVIDENCE,"Found Erik's blood near the entrance"))
  assertEquals(1,gate.visible(updated,"daniel").size)
 }
 @Test fun absentCharacterCanActOffscreen(){
  val erik=Character("erik","Erik",goals=listOf("Explore the ruins"))
  val daniel=Character("daniel","Daniel")
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf("erik" to erik,"daniel" to daniel)))
  val actions=OffscreenSimulator().plan(story,presentCharacterIds=setOf("daniel"))
  assertTrue(actions.any{it.intent.characterId=="erik"})
  assertTrue(actions.all{it.hiddenEvent.knownBy==setOf(it.intent.characterId)})
 }
}
