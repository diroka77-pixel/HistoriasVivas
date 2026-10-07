package com.historiasvivas.engine.offscreen
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.discovery.Evidence
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
  val evidence=Evidence("evidence-1","offscreen-trace","Physical evidence of Erik's action",70,2,"daniel",relatedEventId=hidden.id)
  val discovered=KnowledgeGate().discover(
   listOf(hidden),
   Discovery(hidden.id,"daniel",DiscoveryChannel.EVIDENCE,"I found evidence",sourceId=evidence.id),
   DiscoverySources(evidence=mapOf(evidence.id to evidence))
  )
  assertEquals(1,KnowledgeGate().visible(discovered,"daniel").size)
 }
}
