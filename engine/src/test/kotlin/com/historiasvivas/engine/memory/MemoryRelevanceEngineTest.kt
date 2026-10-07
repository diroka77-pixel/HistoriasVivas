package com.historiasvivas.engine.memory
import com.historiasvivas.engine.model.*
import kotlin.test.*

class MemoryRelevanceEngineTest {
 @Test fun oldCausalMemoryCanOutrankRecentNoise(){
  val old=StoryEvent("erik-abandoned",5,"Daniel abandoned Erik injured in the Black Forest",setOf("daniel","erik"),setOf("forest"),true)
  val noise=(91L..100L).map{StoryEvent("quiet-$it",it,"A quiet meal at the settlement")}
  val story=Story("s","Nadir","p",world=WorldState(
   characters=mapOf("daniel" to Character("daniel","Daniel"),"erik" to Character("erik","Erik")),
   pendingConsequences=listOf(Consequence("erik-return","erik-abandoned","Erik may return",80))
  ),memory=StoryMemory(recentEvents=noise,canonicalEvents=listOf(old)),turn=100)
  val selected=MemoryRelevanceEngine().select(story,"Busco a Erik cerca del bosque")
  assertEquals("erik-abandoned",selected.first().event.id)
  assertTrue("consecuencia-pendiente" in selected.first().reasons)
 }
}
