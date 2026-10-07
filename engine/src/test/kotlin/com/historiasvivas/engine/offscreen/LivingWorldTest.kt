package com.historiasvivas.engine.offscreen
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import kotlin.test.*
class LivingWorldTest {
 @Test fun absentNpcCanActWithoutLeakingToProtagonist(){
  val daniel=Character("daniel","Daniel")
  val erik=Character("erik","Erik",goals=listOf("Restore the transmitter"))
  val story=Story("s","Nadir","p",world=WorldState(characters=mapOf("daniel" to daniel,"erik" to erik)))
  val actions=OffscreenSimulator().plan(story,setOf("daniel"),1)
  assertEquals(1,actions.size)
  val hidden=actions.first().hiddenEvent
  assertEquals("erik",hidden.actorId)
  assertTrue(KnowledgeGate().visible(listOf(hidden),"daniel").isEmpty())
  val discovered=KnowledgeGate().discover(listOf(hidden),Discovery(hidden.id,"daniel",DiscoveryChannel.EVIDENCE,"I found evidence"))
  assertEquals(1,KnowledgeGate().visible(discovered,"daniel").size)
 }
}
