package com.historiasvivas.engine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.context.NarrativeContext
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.narrative.ResponseMode
import com.historiasvivas.engine.turn.TurnEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class LivingStoryCycleTest {
 @Test fun severalTurnsAccumulateMeaningWithoutForcingChoices()=runBlocking{
  val seen=mutableListOf<NarrativeContext>()
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest):NarrativeResponse{
    seen+=request.context
    return when(request.story.turn){
     0L->NarrativeResponse(
      prose="Elena listens but does not answer immediately.",
      eventSummary="Daniel asks Elena about Erik.",
      participantIds=setOf("daniel","elena"),
      proposedPerceptionDeltas=listOf(PerceptionDelta("elena","daniel",mapOf("trustworthy" to 3)))
     )
     1L->NarrativeResponse(
      prose="Daniel lets the silence remain.",
      eventSummary="Daniel watches the rain and waits.",
      participantIds=setOf("daniel")
     )
     else->NarrativeResponse(
      prose="Daniel admits that he abandoned Erik.",
      eventSummary="Daniel confesses abandoning Erik.",
      participantIds=setOf("daniel","elena"),
      canonical=true,
      proposedMindDeltas=listOf(MindDelta("daniel",mapOf("compassion" to -4),memoriesToAdd=setOf("Confessed abandoning Erik"))),
      proposedPerceptionDeltas=listOf(PerceptionDelta("elena","daniel",mapOf("trustworthy" to -10)))
     )
    }
   }
  }
  var story=Story("s","Living Story","Premise",world=WorldState(characters=mapOf(
   "daniel" to Character("daniel","Daniel",mind=CharacterMind(personality=PersonalityProfile(mapOf("compassion" to 60)))),
   "elena" to Character("elena","Elena")
  )))
  story=TurnEngine(ai).play(story,"Pregunto a Elena qué sabe de Erik.").story
  story=TurnEngine(ai).play(story,"Miro la lluvia y espero.").story
  story=TurnEngine(ai).play(story,"Confieso que abandoné a Erik.").story
  assertEquals(3,story.turn)
  assertEquals(ResponseMode.DEVELOP,seen[0].narrativeBeat.mode)
  assertEquals(ResponseMode.BREATHE,seen[1].narrativeBeat.mode)
  assertEquals(ResponseMode.ESCALATE,seen[2].narrativeBeat.mode)
  assertTrue(seen.all{!it.narrativeBeat.shouldOfferChoices})
  assertTrue(seen.all{it.narrativeBeat.preservePlayerDecisionSpace})
  assertEquals(56,story.world.characters["daniel"]!!.mind.personality.dimensions["compassion"])
  assertEquals(-7,story.world.characters["elena"]!!.mind.perceptions["daniel"]!!.impressions["trustworthy"])
  assertTrue(story.memory.canonicalEvents.any{it.summary.contains("confesses",ignoreCase=true)})
 }
}
