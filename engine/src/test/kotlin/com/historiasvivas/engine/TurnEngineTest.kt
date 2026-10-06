package com.historiasvivas.engine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.turn.TurnEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class TurnEngineTest {
 @Test fun actionCreatesPersistentEventAndDelayedConsequence()= runBlocking {
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="Erik observa en silencio mientras te alejas entre los árboles.",
    eventSummary="El protagonista abandona a Erik herido.",
    participantIds=setOf("erik"),canonical=true,
    proposedFacts=setOf("Erik fue abandonado herido"),
    proposedConsequences=listOf(Consequence("erik-return","turn-1","Erik recordará el abandono",10))
   )
  }
  val start=Story("s1","Bosque","Dos viajeros perdidos.",world=WorldState(characters=mapOf("erik" to Character("erik","Erik"))))
  val result=TurnEngine(ai).play(start,"Dejo a Erik y continúo solo.")
  assertEquals(1,result.story.turn)
  assertTrue("Erik fue abandonado herido" in result.story.world.facts)
  assertTrue(result.story.memory.canonicalEvents.any{it.id=="turn-1"})
  assertTrue(result.story.world.pendingConsequences.any{it.id=="erik-return"&&it.earliestTurn==10L})
 }
}
