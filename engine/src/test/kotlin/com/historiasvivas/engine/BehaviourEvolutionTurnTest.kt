package com.historiasvivas.engine
import com.historiasvivas.engine.ai.*
import com.historiasvivas.engine.character.*
import com.historiasvivas.engine.model.*
import com.historiasvivas.engine.turn.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class BehaviourEvolutionTurnTest {
 @Test fun witnessedCruelActionChangesActorAndWitnessPerception()=runBlocking{
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="Daniel leaves Erik behind. Elena watches in silence.",
    eventSummary="Daniel abandons Erik while Elena witnesses it.",
    participantIds=setOf("daniel","erik","elena"),
    canonical=true,
    proposedMindDeltas=listOf(MindDelta("daniel",mapOf("compassion" to -8,"ruthlessness" to 6),memoriesToAdd=setOf("Abandoned Erik injured"))),
    proposedPerceptionDeltas=listOf(PerceptionDelta("elena","daniel",mapOf("trustworthy" to -12,"dangerous" to 7),setOf("Saw Daniel abandon Erik")))
   )
  }
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf(
   "daniel" to Character("daniel","Daniel",mind=CharacterMind(personality=PersonalityProfile(mapOf("compassion" to 60)))),
   "erik" to Character("erik","Erik"),
   "elena" to Character("elena","Elena")
  )))
  val result=TurnEngine(ai).play(story,"I leave Erik and continue.")
  assertEquals(52,result.story.world.characters["daniel"]!!.mind.personality.dimensions["compassion"])
  assertEquals(-12,result.story.world.characters["elena"]!!.mind.perceptions["daniel"]!!.impressions["trustworthy"])
 }
 @Test fun abruptPersonalityRewriteIsRejected()=runBlocking{
  val ai=object:NarrativeAI{
   override suspend fun continueStory(request:NarrativeRequest)=NarrativeResponse(
    prose="A moment passes.",eventSummary="A moment passes.",
    participantIds=setOf("daniel"),
    proposedMindDeltas=listOf(MindDelta("daniel",mapOf("compassion" to -90)))
   )
  }
  val story=Story("s","Story","Premise",world=WorldState(characters=mapOf("daniel" to Character("daniel","Daniel"))))
  assertFailsWith<IllegalArgumentException>{TurnEngine(ai).play(story,"I frown.")}
 }
}
